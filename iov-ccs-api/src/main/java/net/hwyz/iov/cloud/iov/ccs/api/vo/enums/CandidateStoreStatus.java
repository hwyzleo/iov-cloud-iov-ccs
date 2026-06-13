package net.hwyz.iov.cloud.iov.ccs.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 候选入库状态枚举
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum CandidateStoreStatus {

    /**
     * 待入库
     */
    PENDING("PENDING", "待入库"),

    /**
     * 入库成功
     */
    SUCCESS("SUCCESS", "入库成功"),

    /**
     * 重复
     */
    DUPLICATE("DUPLICATE", "重复"),

    /**
     * 入库失败
     */
    FAILED("FAILED", "入库失败");

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
    public static CandidateStoreStatus fromCode(String code) {
        for (CandidateStoreStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的候选入库状态: " + code);
    }
}
