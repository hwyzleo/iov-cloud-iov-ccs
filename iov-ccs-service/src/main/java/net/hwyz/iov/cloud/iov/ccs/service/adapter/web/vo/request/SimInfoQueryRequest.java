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
     * 来源运营商：CMCC/CUCC/UNKNOWN
     */
    private String sourceMno;

    /**
     * 来源类型：cmcc_file/cucc_push/manual_save/manual_batch/sync_data
     */
    private String sourceType;
}
