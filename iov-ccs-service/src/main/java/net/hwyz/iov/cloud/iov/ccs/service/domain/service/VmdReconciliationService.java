package net.hwyz.iov.cloud.iov.ccs.service.domain.service;

/**
 * VMD对账服务接口
 * <p>
 * 负责启动bootstrap和周期对账，补齐丢事件缺口
 *
 * @author hwyz_leo
 */
public interface VmdReconciliationService {

    /**
     * 启动bootstrap
     * <p>
     * 服务启动调VmdVehiclePartBindingService拉取TBOX绑定全量/增量，补齐丢事件期间的缺口
     */
    void bootstrap();

    /**
     * 周期对账
     * <p>
     * 定时比对VMD绑定现状与tb_vehicle_sim，差异项补绑/告警
     */
    void reconcile();

    /**
     * 处理EOL兜底
     * <p>
     * EOL通信检测失败 → 触发对账 + 重试，不在EOL重建绑定
     *
     * @param vin VIN
     */
    void handleEolFallback(String vin);
}
