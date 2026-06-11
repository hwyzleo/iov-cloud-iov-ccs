package net.hwyz.iov.cloud.iov.ccs.api.vo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SIM卡状态枚举
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum SimState {

    /**
     * 测试
     */
    TEST(1, "测试"),

    /**
     * 库存
     */
    STOCK(2, "库存"),

    /**
     * 激活
     */
    ACTIVE(3, "激活");

    /**
     * 状态值
     */
    private final Integer value;

    /**
     * 状态描述
     */
    private final String description;

    /**
     * 根据值获取枚举
     *
     * @param value 状态值
     * @return 枚举
     */
    public static SimState fromValue(Integer value) {
        for (SimState state : values()) {
            if (state.getValue().equals(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("未知的SIM卡状态: " + value);
    }
}
