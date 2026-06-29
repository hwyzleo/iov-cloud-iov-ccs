package net.hwyz.iov.cloud.iov.ccs.service.domain.service;

import net.hwyz.iov.cloud.iov.ccs.service.domain.event.VmdBindingEvent;

/**
 * 车卡绑定服务接口
 * <p>
 * 负责处理VMD绑定事件，调用车营商车卡绑定/入库接口
 *
 * @author hwyz_leo
 */
public interface CardBindingService {

    /**
     * 处理VMD绑定事件
     * <p>
     * WHEN changeType=BIND：按ICCID查tb_sim_info → 调运营商车卡绑定/入库接口 → 回写binding_status并落tb_vehicle_sim
     * IF ICCID不在tb_sim_info：发告警 + 触发卡信息补拉，不建立非法绑定
     * IF 运营商调用失败：退避重试 + 告警，不阻断整车下线
     *
     * @param event VMD绑定事件
     */
    void handleBindingEvent(VmdBindingEvent event);

    /**
     * 绑定单张SIM卡
     * <p>
     * 按ICCID路由到对应运营商适配器调用车卡绑定接口
     *
     * @param vin         VIN
     * @param iccid       ICCID
     * @param cardSlot    卡槽（1或2）
     * @param tboxSn      TBOX序列号
     * @param bindingId   绑定ID
     * @param seq         事件序列号
     * @return 是否绑定成功
     */
    boolean bindSingleCard(String vin, String iccid, Integer cardSlot, String tboxSn, String bindingId, Long seq);

    /**
     * 检查ICCID是否已绑定
     *
     * @param iccid ICCID
     * @return 是否已绑定
     */
    boolean isIccidBound(String iccid);
}
