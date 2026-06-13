package net.hwyz.iov.cloud.iov.ccs.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 运营商类型枚举
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum MnoType {

    /**
     * 中国移动
     */
    CMCC("CMCC", "中国移动"),

    /**
     * 中国联通
     */
    CUCC("CUCC", "中国联通"),

    /**
     * 未知
     */
    UNKNOWN("UNKNOWN", "未知");

    /**
     * 运营商代码
     */
    private final String code;

    /**
     * 运营商描述
     */
    private final String description;

    /**
     * 根据代码获取枚举
     *
     * @param code 运营商代码
     * @return 枚举
     */
    public static MnoType fromCode(String code) {
        for (MnoType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的运营商类型: " + code);
    }
}
