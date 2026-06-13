package net.hwyz.iov.cloud.iov.ccs.service.domain.service.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("日志脱敏工具类测试")
class LogMaskUtilTest {

    @Test
    @DisplayName("脱敏MSISDN - 标准11位手机号")
    void maskMsisdn_standard11() {
        String result = LogMaskUtil.maskMsisdn("13800138000");
        assertEquals("138****8000", result);
    }

    @Test
    @DisplayName("脱敏MSISDN - 13位含国家码")
    void maskMsisdn_withCountryCode() {
        String result = LogMaskUtil.maskMsisdn("8613800138000");
        assertEquals("861******8000", result);
    }

    @Test
    @DisplayName("脱敏IMSI - 标准15位")
    void maskImsi_standard() {
        String result = LogMaskUtil.maskImsi("460001234567890");
        assertEquals("460********7890", result);
    }

    @Test
    @DisplayName("脱敏ICCID - 标准20位")
    void maskIccid_standard() {
        String result = LogMaskUtil.maskIccid("89860123456789012345");
        assertEquals("898*************2345", result);
    }

    @Test
    @DisplayName("脱敏 - null值返回null")
    void mask_null() {
        assertNull(LogMaskUtil.mask(null));
    }

    @Test
    @DisplayName("脱敏 - 空字符串返回空字符串")
    void mask_empty() {
        assertEquals("", LogMaskUtil.mask(""));
    }

    @Test
    @DisplayName("脱敏 - 短字符串全部脱敏")
    void mask_shortString() {
        String result = LogMaskUtil.mask("123456");
        assertEquals("******", result);
    }

    @Test
    @DisplayName("脱敏 - 恰好7位字符串")
    void mask_exactly7() {
        String result = LogMaskUtil.mask("1234567");
        assertEquals("1234567", result);
    }

    @Test
    @DisplayName("脱敏JSON - 替换msisdn/imsi/iccid字段")
    void maskJson() {
        String json = "{\"iccid\":\"89860123456789012345\",\"imsi\":\"460001234567890\",\"msisdn\":\"13800138000\"}";
        String masked = LogMaskUtil.maskJson(json);
        assertTrue(masked.contains("138****8000"), "msisdn应被脱敏");
        assertTrue(masked.contains("460********7890"), "imsi应被脱敏");
        assertTrue(masked.contains("898*************2345"), "iccid应被脱敏");
        // 原始值不应出现
        assertFalse(masked.contains("13800138000"));
        assertFalse(masked.contains("460001234567890"));
    }

    @Test
    @DisplayName("脱敏JSON - null返回null")
    void maskJson_null() {
        assertNull(LogMaskUtil.maskJson(null));
    }

    @Test
    @DisplayName("脱敏JSON - 空JSON不影响")
    void maskJson_emptyJson() {
        assertEquals("{}", LogMaskUtil.maskJson("{}"));
    }
}
