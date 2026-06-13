package net.hwyz.iov.cloud.iov.ccs.service.adapter.web.vo.request;

import lombok.Data;

/**
 * SIM信息查询请求
 * <p>
 * 封装列表查询的筛选条件
 *
 * @author hwyz_leo
 */
@Data
public class SimInfoQueryRequest {

    /**
     * ICCID（模糊匹配）
     */
    private String iccid;

    /**
     * IMSI（模糊匹配）
     */
    private String imsi;

    /**
     * MSISDN（模糊匹配）
     */
    private String msisdn;

    /**
     * 来源运营商：CMCC/CUCC/MANUAL
     */
    private String sourceMno;
}
