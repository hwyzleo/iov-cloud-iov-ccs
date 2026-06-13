package net.hwyz.iov.cloud.iov.ccs.service.adapter.web.vo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * SIM信息请求
 *
 * @author hwyz_leo
 */
@Data
public class SimInfoRequest {

    /**
     * ICCID（集成电路卡识别码）
     * 19-20位数字
     */
    @NotBlank(message = "ICCID不能为空")
    @Pattern(regexp = "^\\d{19,20}$", message = "ICCID必须为19-20位数字")
    private String iccid;

    /**
     * IMSI（国际移动用户识别码）
     * 15位数字
     */
    @NotBlank(message = "IMSI不能为空")
    @Pattern(regexp = "^\\d{15}$", message = "IMSI必须为15位数字")
    private String imsi;

    /**
     * MSISDN（手机号码）
     * 支持纯号码或86前缀
     */
    @NotBlank(message = "MSISDN不能为空")
    @Pattern(regexp = "^(86)?1[3-9]\\d{9}$", message = "MSISDN格式不正确")
    private String msisdn;

    /**
     * 运营商类型：CMCC/CUCC/MANUAL
     */
    @NotBlank(message = "运营商类型不能为空")
    private String mnoType;
}
