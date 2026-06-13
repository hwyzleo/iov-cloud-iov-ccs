package net.hwyz.iov.cloud.iov.ccs.service.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 业务告警事件
 * <p>
 * 当发生异常情况时发布此事件
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessAlertEvent {

    /**
     * 告警类型
     */
    private AlertType alertType;

    /**
     * 关联键（fileId/batchNo/requestId等）
     */
    private String refKey;

    /**
     * 告警消息
     */
    private String message;

    /**
     * 详细信息
     */
    private String detail;

    /**
     * 发生时间
     */
    private LocalDateTime happenTime;

    /**
     * 告警类型枚举
     */
    public enum AlertType {
        /**
         * CMCC文件请求失败
         */
        CMCC_FILE_REQUEST_FAILED,

        /**
         * CMCC文件下载失败
         */
        CMCC_FILE_DOWNLOAD_FAILED,

        /**
         * CMCC文件解密失败
         */
        CMCC_FILE_DECRYPT_FAILED,

        /**
         * CMCC文件解析失败
         */
        CMCC_FILE_PARSE_FAILED,

        /**
         * SIM入库失败
         */
        SIM_STORE_FAILED,

        /**
         * SIM数据差异告警（IMSI/MSISDN不一致）
         */
        SIM_DATA_MISMATCH,

        /**
         * CUCC验签失败
         */
        CUCC_SIGNATURE_FAILED,

        /**
         * CUCC重放攻击
         */
        CUCC_REPLAY_ATTACK
    }
}
