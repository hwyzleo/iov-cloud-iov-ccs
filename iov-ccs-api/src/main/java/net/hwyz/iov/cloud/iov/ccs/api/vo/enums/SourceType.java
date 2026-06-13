package net.hwyz.iov.cloud.iov.ccs.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据来源类型枚举
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum SourceType {

    /**
     * 中国移动文件导入
     */
    CMCC_FILE("cmcc_file", "中国移动文件导入"),

    /**
     * 中国联通推送
     */
    CUCC_PUSH("cucc_push", "中国联通推送"),

    /**
     * 手动录入
     */
    MANUAL_SAVE("manual_save", "手动录入"),

    /**
     * 批量录入
     */
    MANUAL_BATCH("manual_batch", "批量录入"),

    /**
     * 数据同步
     */
    SYNC_DATA("sync_data", "数据同步");

    /**
     * 来源类型代码
     */
    private final String code;

    /**
     * 来源类型描述
     */
    private final String description;

    /**
     * 根据代码获取枚举
     *
     * @param code 来源类型代码
     * @return 枚举
     */
    public static SourceType fromCode(String code) {
        for (SourceType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的来源类型: " + code);
    }
}
