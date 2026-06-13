package net.hwyz.iov.cloud.iov.ccs.service.adapter.web.vo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * SIM信息请求
 *
 * @author hwyz_leo
 */
@Data
public class SimInfoRequest {

    /**
     * ICCID
     */
    @NotBlank(message = "ICCID不能为空")
    private String iccid;

    /**
     * IMSI
     */
    @NotBlank(message = "IMSI不能为空")
    private String imsi;

    /**
     * MSISDN
     */
    @NotBlank(message = "MSISDN不能为空")
    private String msisdn;

    /**
     * 运营商类型：CMCC/CUCC/MANUAL
     */
    @NotBlank(message = "运营商类型不能为空")
    private String mnoType;
}
