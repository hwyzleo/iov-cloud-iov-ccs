package net.hwyz.iov.cloud.iov.ccs.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ccs.api.vo.enums.MnoType;
import net.hwyz.iov.cloud.iov.ccs.service.domain.event.BusinessAlertEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.event.SimStorageEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.SimInfoRepository;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SIM信息服务实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimInfoServiceImpl implements SimInfoService {

    private final SimInfoRepository simInfoRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Override
    @Async("eventThreadPool")
    @EventListener
    @Transactional
    public void handleStorageEvent(SimStorageEvent event) {
        log.info("处理SIM存储事件: batchType={}, batchNo={}, sourceMno={}, count={}",
                event.getBatchType(), event.getBatchNo(), event.getSourceMno(),
                event.getSimInfoList() != null ? event.getSimInfoList().size() : 0);

        if (event.getSimInfoList() == null || event.getSimInfoList().isEmpty()) {
            log.info("SIM列表为空，跳过处理");
            return;
        }

        int successCount = 0;
        int duplicateCount = 0;
        int failedCount = 0;

        for (SimInfo simInfo : event.getSimInfoList()) {
            try {
                // 检查ICCID是否已存在
                SimInfo existingSim = simInfoRepository.getByIccid(simInfo.getIccid());

                if (existingSim != null) {
                    // ICCID已存在
                    if (MnoType.CMCC.getCode().equals(event.getSourceMno())
                            || MnoType.CUCC.getCode().equals(event.getSourceMno())) {
                        // CMCC/CUCC：幂等跳过，不覆盖
                        log.debug("ICCID已存在，跳过: iccid={}", simInfo.getIccid());
                        duplicateCount++;

                        // 检查数据差异
                        checkDataDifference(existingSim, simInfo);
                    } else {
                        // 手动/同步等其他来源：按业务逻辑处理
                        duplicateCount++;
                    }
                } else {
                    // ICCID不存在，插入新记录
                    simInfoRepository.save(simInfo);
                    successCount++;
                    log.debug("SIM信息保存成功: iccid={}", simInfo.getIccid());
                }
            } catch (Exception e) {
                failedCount++;
                log.error("SIM信息保存失败: iccid={}", simInfo.getIccid(), e);

                // 发布告警事件
                eventPublisher.publishEvent(BusinessAlertEvent.builder()
                        .alertType(BusinessAlertEvent.AlertType.SIM_STORE_FAILED)
                        .refKey(event.getBatchNo())
                        .message("SIM入库失败: " + simInfo.getIccid())
                        .detail(e.getMessage())
                        .happenTime(LocalDateTime.now())
                        .build());
            }
        }

        log.info("SIM存储事件处理完成: batchNo={}, success={}, duplicate={}, failed={}",
                event.getBatchNo(), successCount, duplicateCount, failedCount);
    }

    @Override
    @Transactional
    public boolean saveSimInfo(SimInfo simInfo) {
        // 检查ICCID是否已存在
        if (simInfoRepository.existsByIccid(simInfo.getIccid())) {
            log.warn("ICCID已存在，拒绝保存: iccid={}", simInfo.getIccid());
            return false;
        }

        simInfoRepository.save(simInfo);
        return true;
    }

    @Override
    @Transactional
    public void upsertSimInfo(SimInfo simInfo) {
        SimInfo existingSim = simInfoRepository.getByIccid(simInfo.getIccid());

        if (existingSim != null) {
            // 更新：允许覆盖 IMSI/MSISDN/source_*
            existingSim.setImsi(simInfo.getImsi());
            existingSim.setMsisdn(simInfo.getMsisdn());
            existingSim.setSourceMno(simInfo.getSourceMno());
            existingSim.setSourceType(simInfo.getSourceType());
            existingSim.setSourceRef(simInfo.getSourceRef());
            simInfoRepository.update(existingSim);
            log.debug("SIM信息更新成功: iccid={}", simInfo.getIccid());
        } else {
            // 插入
            simInfoRepository.save(simInfo);
            log.debug("SIM信息插入成功: iccid={}", simInfo.getIccid());
        }
    }

    /**
     * 检查数据差异
     * <p>
     * 如果新输入与已存 IMSI/MSISDN 不一致，记录告警事件
     */
    private void checkDataDifference(SimInfo existing, SimInfo incoming) {
        boolean imsiMismatch = existing.getImsi() != null && incoming.getImsi() != null
                && !existing.getImsi().equals(incoming.getImsi());
        boolean msisdnMismatch = existing.getMsisdn() != null && incoming.getMsisdn() != null
                && !existing.getMsisdn().equals(incoming.getMsisdn());

        if (imsiMismatch || msisdnMismatch) {
            log.warn("SIM数据差异: iccid={}, imsiMatch={}, msisdnMatch={}",
                    existing.getIccid(), !imsiMismatch, !msisdnMismatch);

            // 发布告警事件（日志不含明文）
            eventPublisher.publishEvent(BusinessAlertEvent.builder()
                    .alertType(BusinessAlertEvent.AlertType.SIM_DATA_MISMATCH)
                    .refKey(existing.getIccid())
                    .message("SIM数据差异告警")
                    .detail("ICCID=" + existing.getIccid() + ", imsiMatch=" + !imsiMismatch + ", msisdnMatch=" + !msisdnMismatch)
                    .happenTime(LocalDateTime.now())
                    .build());
        }
    }
}
