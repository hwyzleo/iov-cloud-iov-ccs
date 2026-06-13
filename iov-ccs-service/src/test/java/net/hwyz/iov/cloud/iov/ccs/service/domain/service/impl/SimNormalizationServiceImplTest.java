package net.hwyz.iov.cloud.iov.ccs.service.domain.service.impl;

import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SIM数据规范化服务测试")
class SimNormalizationServiceImplTest {

    private SimNormalizationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SimNormalizationServiceImpl();
    }

    // ========== validateIccid ==========

    @Test
    @DisplayName("ICCID校验 - 有效20位数字")
    void validateIccid_valid20() {
        assertTrue(service.validateIccid("89860123456789012345"));
    }

    @Test
    @DisplayName("ICCID校验 - 有效19位数字")
    void validateIccid_valid19() {
        assertTrue(service.validateIccid("8986012345678901234"));
    }

    @Test
    @DisplayName("ICCID校验 - 无效null")
    void validateIccid_null() {
        assertFalse(service.validateIccid(null));
    }

    @Test
    @DisplayName("ICCID校验 - 无效空字符串")
    void validateIccid_empty() {
        assertFalse(service.validateIccid(""));
    }

    @Test
    @DisplayName("ICCID校验 - 无效含字母")
    void validateIccid_withLetters() {
        assertFalse(service.validateIccid("8986012345678901234A"));
    }

    @Test
    @DisplayName("ICCID校验 - 无效长度不足")
    void validateIccid_tooShort() {
        assertFalse(service.validateIccid("12345"));
    }

    // ========== validateImsi ==========

    @Test
    @DisplayName("IMSI校验 - 有效15位数字")
    void validateImsi_valid() {
        assertTrue(service.validateImsi("460001234567890"));
    }

    @Test
    @DisplayName("IMSI校验 - 无效null")
    void validateImsi_null() {
        assertFalse(service.validateImsi(null));
    }

    @Test
    @DisplayName("IMSI校验 - 无效14位")
    void validateImsi_short() {
        assertFalse(service.validateImsi("46000123456789"));
    }

    @Test
    @DisplayName("IMSI校验 - 无效16位")
    void validateImsi_long() {
        assertFalse(service.validateImsi("4600012345678901"));
    }

    // ========== validateMsisdn ==========

    @Test
    @DisplayName("MSISDN校验 - 有效11位")
    void validateMsisdn_valid11() {
        assertTrue(service.validateMsisdn("13800138000"));
    }

    @Test
    @DisplayName("MSISDN校验 - 有效13位含国家码")
    void validateMsisdn_valid13() {
        assertTrue(service.validateMsisdn("8613800138000"));
    }

    @Test
    @DisplayName("MSISDN校验 - 无效null")
    void validateMsisdn_null() {
        assertFalse(service.validateMsisdn(null));
    }

    @Test
    @DisplayName("MSISDN校验 - 无效12位")
    void validateMsisdn_invalid12() {
        assertFalse(service.validateMsisdn("138001380001"));
    }

    // ========== normalizeMsisdn ==========

    @Test
    @DisplayName("MSISDN规范化 - 11位拼接86")
    void normalizeMsisdn_11digit() {
        assertEquals("8613800138000", service.normalizeMsisdn("13800138000"));
    }

    @Test
    @DisplayName("MSISDN规范化 - 已有86前缀保持不变")
    void normalizeMsisdn_alreadyWith86() {
        assertEquals("8613800138000", service.normalizeMsisdn("8613800138000"));
    }

    @Test
    @DisplayName("MSISDN规范化 - null返回null")
    void normalizeMsisdn_null() {
        assertNull(service.normalizeMsisdn(null));
    }

    @Test
    @DisplayName("MSISDN规范化 - 空字符串返回空字符串")
    void normalizeMsisdn_empty() {
        assertEquals("", service.normalizeMsisdn(""));
    }

    // ========== normalize ==========

    @Test
    @DisplayName("规范化 - 正常数据")
    void normalize_valid() {
        SimInfo input = SimInfo.builder()
                .iccid("89860123456789012345")
                .imsi("460001234567890")
                .msisdn("13800138000")
                .build();

        SimInfo result = service.normalize(input);

        assertEquals("89860123456789012345", result.getIccid());
        assertEquals("460001234567890", result.getImsi());
        assertEquals("8613800138000", result.getMsisdn());
    }

    @Test
    @DisplayName("规范化 - 带前后空白")
    void normalize_withSpaces() {
        SimInfo input = SimInfo.builder()
                .iccid(" 89860123456789012345 ")
                .imsi(" 460001234567890 ")
                .msisdn(" 13800138000 ")
                .build();

        SimInfo result = service.normalize(input);

        assertEquals("89860123456789012345", result.getIccid());
        assertEquals("460001234567890", result.getImsi());
        assertEquals("8613800138000", result.getMsisdn());
    }

    @Test
    @DisplayName("规范化 - 保留来源字段")
    void normalize_preservesSourceFields() {
        SimInfo input = SimInfo.builder()
                .iccid("89860123456789012345")
                .imsi("460001234567890")
                .msisdn("13800138000")
                .sourceMno("CMCC")
                .sourceType("cmcc_file")
                .sourceRef("file_001")
                .build();

        SimInfo result = service.normalize(input);

        assertEquals("CMCC", result.getSourceMno());
        assertEquals("cmcc_file", result.getSourceType());
        assertEquals("file_001", result.getSourceRef());
    }

    @Test
    @DisplayName("规范化 - ICCID为空抛异常")
    void normalize_nullIccid() {
        SimInfo input = SimInfo.builder()
                .iccid(null)
                .imsi("460001234567890")
                .msisdn("13800138000")
                .build();

        assertThrows(IllegalArgumentException.class, () -> service.normalize(input));
    }

    @Test
    @DisplayName("规范化 - IMSI格式无效抛异常")
    void normalize_invalidImsi() {
        SimInfo input = SimInfo.builder()
                .iccid("89860123456789012345")
                .imsi("invalid_imsi")
                .msisdn("13800138000")
                .build();

        assertThrows(IllegalArgumentException.class, () -> service.normalize(input));
    }

    @Test
    @DisplayName("规范化 - MSISDN格式无效抛异常")
    void normalize_invalidMsisdn() {
        SimInfo input = SimInfo.builder()
                .iccid("89860123456789012345")
                .imsi("460001234567890")
                .msisdn("123")
                .build();

        assertThrows(IllegalArgumentException.class, () -> service.normalize(input));
    }
}
