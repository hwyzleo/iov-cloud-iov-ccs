package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.mno;

/**
 * 运营商车卡绑定客户端接口
 * <p>
 * 按ICCID路由到对应运营商适配器调用车卡绑定/入库接口
 *
 * @author hwyz_leo
 */
public interface MnoBindingClient {

    /**
     * 车卡绑定/入库
     *
     * @param iccid ICCID
     * @param vin   VIN
     * @return 是否成功
     */
    boolean bindCard(String iccid, String vin);

    /**
     * 获取运营商类型
     *
     * @return 运营商类型（CMCC/CUCC等）
     */
    String getMnoType();
}
