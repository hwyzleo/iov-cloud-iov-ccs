package net.hwyz.iov.cloud.iov.ccs.service.domain.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ccs.service.domain.event.BusinessAlertEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.VehicleSim;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.VehicleSimRepository;
import net.hwyz.iov.cloud.iov.ccs.service.domain.service.CardBindingService;
import net.hwyz.iov.cloud.iov.ccs.service.domain.service.VmdReconciliationService;
import net.hwyz.iov.cloud.iov.ccs.service.domain.service.metrics.CcsMetricsService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * VMD对账服务实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VmdReconciliationServiceImpl implements VmdReconciliationService {

    private static final int DIFF_THRESHOLD = 10;

    private final VehicleSimRepository vehicleSimRepository;
    private final CardBindingService cardBindingService;
    private final ApplicationEventPublisher eventPublisher;
    private final CcsMetricsService metricsService;

    @Override
    public void bootstrap() {
        log.info("开始VMD bootstrap对账");

        try {
            // TODO: 调用VmdVehiclePartBindingService拉取TBOX绑定全量/增量
            List<VmdBindingInfo> vmdBindings = fetchVmdBindings();

            // 获取本地绑定记录
            List<VehicleSim> localBindings = vehicleSimRepository.listAll();
            Map<String, VehicleSim> localBindingMap = localBindings.stream()
                    .collect(Collectors.toMap(VehicleSim::getIccid, v -> v));

            int catchUpCount = 0;
            int alertCount = 0;

            // 比对差异
            for (VmdBindingInfo vmdBinding : vmdBindings) {
                VehicleSim localBinding = localBindingMap.get(vmdBinding.iccid());

                if (localBinding == null) {
                    // 本地无记录，需要补绑
                    log.info("发现缺失绑定，触发补绑: vin={}, iccid={}", vmdBinding.vin(), vmdBinding.iccid());
                    boolean success = cardBindingService.bindSingleCard(
                            vmdBinding.vin(),
                            vmdBinding.iccid(),
                            vmdBinding.cardSlot(),
                            vmdBinding.tboxSn(),
                            "bootstrap-" + System.currentTimeMillis(),
                            0L
                    );
                    if (success) {
                        catchUpCount++;
                    } else {
                        alertCount++;
                    }
                }
            }

            metricsService.recordVmdReconciliationCatchUp(catchUpCount);
            metricsService.recordVmdReconciliationAlert(alertCount);

            log.info("VMD bootstrap对账完成: catchUp={}, alert={}", catchUpCount, alertCount);

        } catch (Exception e) {
            log.error("VMD bootstrap对账失败", e);
            eventPublisher.publishEvent(BusinessAlertEvent.builder()
                    .alertType(BusinessAlertEvent.AlertType.VMD_RECONCILIATION_FAIL)
                    .message("VMD bootstrap对账失败")
                    .detail(e.getMessage())
                    .happenTime(LocalDateTime.now())
                    .build());
        }
    }

    @Override
    @Scheduled(cron = "${ccs.reconciliation.cron:0 0 2 * * ?}")
    public void reconcile() {
        log.info("开始VMD周期对账");

        try {
            // TODO: 调用VmdVehiclePartBindingService获取当前绑定状态
            List<VmdBindingInfo> vmdBindings = fetchVmdBindings();

            // 获取本地绑定记录
            List<VehicleSim> localBindings = vehicleSimRepository.listAll();
            Map<String, VehicleSim> localBindingMap = localBindings.stream()
                    .collect(Collectors.toMap(VehicleSim::getIccid, v -> v));

            int diffCount = 0;
            int rebindCount = 0;

            // 比对差异
            for (VmdBindingInfo vmdBinding : vmdBindings) {
                VehicleSim localBinding = localBindingMap.get(vmdBinding.iccid());

                if (localBinding == null) {
                    // 本地无记录，需要补绑
                    log.warn("对账发现缺失绑定: vin={}, iccid={}", vmdBinding.vin(), vmdBinding.iccid());
                    diffCount++;

                    boolean success = cardBindingService.bindSingleCard(
                            vmdBinding.vin(),
                            vmdBinding.iccid(),
                            vmdBinding.cardSlot(),
                            vmdBinding.tboxSn(),
                            "reconciliation-" + System.currentTimeMillis(),
                            0L
                    );
                    if (success) {
                        rebindCount++;
                    }
                }
            }

            metricsService.recordVmdReconciliationDiff(diffCount);
            metricsService.recordVmdReconciliationRebind(rebindCount);

            log.info("VMD周期对账完成: diff={}, rebind={}", diffCount, rebindCount);

            // 对账差异超阈值告警
            if (diffCount > DIFF_THRESHOLD) {
                eventPublisher.publishEvent(BusinessAlertEvent.builder()
                        .alertType(BusinessAlertEvent.AlertType.VMD_RECONCILIATION_DIFF_THRESHOLD)
                        .message("VMD对账差异超阈值")
                        .detail("差异数量: " + diffCount)
                        .happenTime(LocalDateTime.now())
                        .build());
            }

        } catch (Exception e) {
            log.error("VMD周期对账失败", e);
            eventPublisher.publishEvent(BusinessAlertEvent.builder()
                    .alertType(BusinessAlertEvent.AlertType.VMD_RECONCILIATION_FAIL)
                    .message("VMD周期对账失败")
                    .detail(e.getMessage())
                    .happenTime(LocalDateTime.now())
                    .build());
        }
    }

    @Override
    public void handleEolFallback(String vin) {
        log.info("处理EOL兜底: vin={}", vin);

        // TODO: 实现EOL通信检测校验与对账兜底
        // EOL通信检测失败 → 触发对账 + 重试，不在EOL重建绑定
    }

    /**
     * 从VMD获取绑定信息
     */
    private List<VmdBindingInfo> fetchVmdBindings() {
        // TODO: 实现真实的VMD绑定信息获取
        // Mock实现
        return List.of();
    }

    /**
     * VMD绑定信息
     */
    private record VmdBindingInfo(
            String vin,
            String iccid,
            Integer cardSlot,
            String tboxSn
    ) {}
}
