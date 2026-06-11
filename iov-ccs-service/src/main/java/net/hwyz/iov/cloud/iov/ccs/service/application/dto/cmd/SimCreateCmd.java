package net.hwyz.iov.cloud.iov.ccs.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SIM卡创建命令
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimCreateCmd {

    /**
     * 集成电路卡识别码
     */
    private String iccid;

    /**
     * 国际移动用户识别号
     */
    private String imsi;

    /**
     * 手机号
     */
    private String msisdn;

    /**
     * 运营商编码
     */
    private String mnoCode;

    /**
     * 备注
     */
    private String description;

    /**
     * 创建者
     */
    private String createBy;
}
