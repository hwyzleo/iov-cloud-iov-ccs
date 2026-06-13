package net.hwyz.iov.cloud.iov.ccs.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * CMCC请求状态枚举
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum CmccRequestStatus {

    /**
     * 申请中
     */
    APPLYING("APPLYING", "申请中"),

    /**
     * 已通知
     */
    NOTIFIED("NOTIFIED", "已通知"),

    /**
     * 已下载
     */
    DOWNLOADED("DOWNLOADED", "已下载"),

    /**
     * 已解密
     */
    DECRYPTED("DECRYPTED", "已解密"),

    /**
     * 已解析
     */
    PARSED("PARSED", "已解析"),

    /**
     * 已入库
     */
    STORED("STORED", "已入库"),

    /**
     * 失败
     */
    FAILED("FAILED", "失败");

    /**
     * 状态代码
     */
    private final String code;

    /**
     * 状态描述
     */
    private final String description;

    /**
     * 根据代码获取枚举
     *
     * @param code 状态代码
     * @return 枚举
     */
    public static CmccRequestStatus fromCode(String code) {
        for (CmccRequestStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的CMCC请求状态: " + code);
    }
}
