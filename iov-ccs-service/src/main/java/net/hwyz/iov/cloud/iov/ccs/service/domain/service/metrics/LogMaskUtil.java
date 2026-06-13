package net.hwyz.iov.cloud.iov.ccs.service.domain.service.metrics;

/**
 * 日志脱敏工具类
 * <p>
 * MSISDN/IMSI仅显示后4位，其他用*号代替
 *
 * @author hwyz_leo
 */
public final class LogMaskUtil {

    private static final int VISIBLE_SUFFIX_LENGTH = 4;
    private static final char MASK_CHAR = '*';

    private LogMaskUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 脱敏MSISDN
     * <p>
     * 示例：13800138000 → 138****8000
     *
     * @param msisdn 手机号
     * @return 脱敏后的手机号
     */
    public static String maskMsisdn(String msisdn) {
        return mask(msisdn);
    }

    /**
     * 脱敏IMSI
     * <p>
     * 示例：460001234567890 → 4600012****7890
     *
     * @param imsi IMSI
     * @return 脱敏后的IMSI
     */
    public static String maskImsi(String imsi) {
        return mask(imsi);
    }

    /**
     * 脱敏ICCID
     * <p>
     * 示例：89860123456789012345 → 898601234567890****45
     *
     * @param iccid ICCID
     * @return 脱敏后的ICCID
     */
    public static String maskIccid(String iccid) {
        return mask(iccid);
    }

    /**
     * 通用脱敏方法
     * <p>
     * 保留前3位和后4位，中间用*号代替
     * 如果长度不足7位，则全部用*号代替
     *
     * @param value 原始值
     * @return 脱敏后的值
     */
    public static String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        int length = value.length();

        // 长度不足7位，全部脱敏
        if (length < 7) {
            return MASK_CHAR + String.valueOf(MASK_CHAR).repeat(length - 1);
        }

        // 保留前3位和后4位
        int prefixLength = 3;
        int suffixLength = VISIBLE_SUFFIX_LENGTH;
        int maskLength = length - prefixLength - suffixLength;

        StringBuilder sb = new StringBuilder();
        sb.append(value, 0, prefixLength);
        sb.append(String.valueOf(MASK_CHAR).repeat(maskLength));
        sb.append(value, length - suffixLength, length);

        return sb.toString();
    }

    /**
     * 脱敏JSON字符串中的敏感字段
     * <p>
     * 识别 "msisdn": "xxx", "imsi": "xxx", "iccid": "xxx" 格式
     *
     * @param json JSON字符串
     * @return 脱敏后的JSON字符串
     */
    public static String maskJson(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        // 简单实现：替换常见字段值
        String result = json;

        // 匹配 "msisdn": "xxx" 或 "msisdn":"xxx"
        result = result.replaceAll("(\"msisdn\"\\s*:\\s*\")([^\"]+)(\")",
                "$1" + mask("$2") + "$3");

        // 匹配 "imsi": "xxx" 或 "imsi":"xxx"
        result = result.replaceAll("(\"imsi\"\\s*:\\s*\")([^\"]+)(\")",
                "$1" + mask("$2") + "$3");

        // 匹配 "iccid": "xxx" 或 "iccid":"xxx"
        result = result.replaceAll("(\"iccid\"\\s*:\\s*\")([^\"]+)(\")",
                "$1" + mask("$2") + "$3");

        return result;
    }
}
