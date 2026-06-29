package net.hwyz.iov.cloud.iov.ccs.service.domain.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ccs.service.domain.event.BusinessAlertEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.event.VmdBindingEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.VehicleSim;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.SimInfoRepository;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.VehicleSimRepository;
import net.hwyz.iov.cloud.iov.ccs.service.domain.service.CardBindingService;
import net.hwyz.iov.cloud.iov.ccs.service.domain.service.LockService;
import net.hwyz.iov.cloud.iov.ccs.service.domain.service.metrics.CcsMetricsService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 车卡绑定服务实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardBindingServiceImpl implements CardBindingService {

    private static final String BIND = "BIND";
    private static final Integer BOUND = 1;
    private static final Integer UNBOUNDED = 0;
    private static final String PACKAGE_TYPE_TEST = "TEST";

    private final SimInfoRepository simInfoRepository;
    private final VehicleSimRepository vehicleSimRepository;
    private final LockService lockService;
    private final ApplicationEventPublisher eventPublisher;
    private final CcsMetricsService metricsService;

    @Override
    @Transactional
    public void handleBindingEvent(VmdBindingEvent event) {
        log.info("处理VMD绑定事件: bindingId={}, changeType={}, vin={}, iccid1={}, iccid2={}",
                event.getBindingId(), event.getChangeType(), event.getVin(),
                event.getIccid1(), event.getIccid2());

        // 幂等检查：检查是否已处理过该事件
        if (isEventProcessed(event.getBindingId(), event.getChangeType())) {
            log.info("事件已处理过，跳过: bindingId={}, changeType={}", event.getBindingId(), event.getChangeType());
            return;
        }

        // 乱序检查：比较seq，旧事件不覆盖新状态
        if (isOutOfOrder(event.getVin(), event.getSeq())) {
            log.warn("事件乱序，跳过: bindingId={}, seq={}, vin={}", event.getBindingId(), event.getSeq(), event.getVin());
            return;
        }

        if (!BIND.equals(event.getChangeType())) {
            log.info("非绑定事件，跳过: changeType={}", event.getChangeType());
            return;
        }

        // 处理ICCID1（卡槽1）
        if (event.getIccid1() != null && !event.getIccid1().isEmpty()) {
            bindSingleCard(event.getVin(), event.getIccid1(), 1, event.getTboxSn(),
                    event.getBindingId(), event.getSeq());
        }

        // 处理ICCID2（卡槽2）
        if (event.getIccid2() != null && !event.getIccid2().isEmpty()) {
            bindSingleCard(event.getVin(), event.getIccid2(), 2, event.getTboxSn(),
                    event.getBindingId(), event.getSeq());
        }
    }

    @Override
    @Transactional
    public boolean bindSingleCard(String vin, String iccid, Integer cardSlot, String tboxSn,
                                   String bindingId, Long seq) {
        log.info("绑定单张SIM卡: vin={}, iccid={}, cardSlot={}", vin, iccid, cardSlot);

        // 使用分布式锁保证同一ICCID并发安全
        String lockKey = "ICCID:" + iccid;
        try {
            return lockService.executeWithLock(lockKey, 60, () -> {
                // 检查ICCID是否已在tb_sim_info中
                SimInfo simInfo = simInfoRepository.getByIccid(iccid);
                if (simInfo == null) {
                    log.warn("ICCID不在tb_sim_info中，发告警: iccid={}", iccid);
                    metricsService.recordVmdBindingIccidNotFound();

                    eventPublisher.publishEvent(BusinessAlertEvent.builder()
                            .alertType(BusinessAlertEvent.AlertType.VMD_BINDING_ICCID_NOT_FOUND)
                            .refKey(iccid)
                            .message("ICCID不在SIM信息表中")
                            .detail("vin=" + vin + ", iccid=" + iccid)
                            .happenTime(LocalDateTime.now())
                            .build());

                    // TODO: 触发卡信息补拉
                    return false;
                }

                // 检查是否已绑定
                VehicleSim existingBinding = vehicleSimRepository.getByIccid(iccid);
                if (existingBinding != null && BOUND.equals(existingBinding.getBindingStatus())) {
                    log.info("ICCID已绑定，跳过: iccid={}, vin={}", iccid, existingBinding.getVin());
                    return true;
                }

                // 调用车营商车卡绑定接口
                boolean bindSuccess = callMnoBindingApi(iccid, vin, simInfo.getSourceMno());
                if (!bindSuccess) {
                    log.error("运营商车卡绑定失败: iccid={}, vin={}", iccid, vin);
                    metricsService.recordVmdBindingMnoFail();

                    eventPublisher.publishEvent(BusinessAlertEvent.builder()
                            .alertType(BusinessAlertEvent.AlertType.VMD_BINDING_MNO_FAIL)
                            .refKey(iccid)
                            .message("运营商车卡绑定失败")
                            .detail("vin=" + vin + ", iccid=" + iccid + ", mno=" + simInfo.getSourceMno())
                            .happenTime(LocalDateTime.now())
                            .build());

                    return false;
                }

                // 回写tb_sim_info.binding_status=BOUNDED
                simInfo.setBindingStatus(BOUND);
                simInfoRepository.update(simInfo);

                // Upsert tb_vehicle_sim
                VehicleSim vehicleSim = VehicleSim.builder()
                        .vin(vin)
                        .iccid(iccid)
                        .cardSlot(cardSlot)
                        .bindingStatus(BOUND)
                        .packageType(PACKAGE_TYPE_TEST)
                        .tboxSn(tboxSn)
                        .sourceEventId(bindingId)
                        .sourceSeq(seq)
                        .boundTime(LocalDateTime.now())
                        .build();
                vehicleSimRepository.upsert(vehicleSim);

                metricsService.recordVmdBindingSuccess();
                log.info("车卡绑定成功: vin={}, iccid={}, cardSlot={}", vin, iccid, cardSlot);
                return true;
            });
        } catch (IllegalStateException e) {
            log.error("获取分布式锁失败: iccid={}", iccid, e);
            metricsService.recordVmdBindingLockFail();
            return false;
        }
    }

    @Override
    public boolean isIccidBound(String iccid) {
        VehicleSim vehicleSim = vehicleSimRepository.getByIccid(iccid);
        return vehicleSim != null && BOUND.equals(vehicleSim.getBindingStatus());
    }

    /**
     * 检查事件是否已处理过（幂等）
     */
    private boolean isEventProcessed(String bindingId, String changeType) {
        // TODO: 实现幂等检查（可通过处理表或Redis缓存实现）
        return false;
    }

    /**
     * 检查是否为乱序事件
     */
    private boolean isOutOfOrder(String vin, Long seq) {
        // TODO: 实现乱序检查（比较seq与当前VIN的最新seq）
        return false;
    }

    /**
     * 调用车营商车卡绑定接口
     * <p>
     * 按ICCID路由到对应运营商适配器
     */
    private boolean callMnoBindingApi(String iccid, String vin, String sourceMno) {
        log.info("调用车营商车卡绑定接口: iccid={}, vin={}, mno={}", iccid, vin, sourceMno);
        // TODO: 实现真实的运营商车卡绑定接口调用
        // 根据sourceMno路由到CMCC/CUCC等不同运营商适配器
        // Mock实现：返回成功
        return true;
    }
}
