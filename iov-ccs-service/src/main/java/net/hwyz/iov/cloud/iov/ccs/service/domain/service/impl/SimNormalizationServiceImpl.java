package net.hwyz.iov.cloud.iov.ccs.service.domain.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;
import net.hwyz.iov.cloud.iov.ccs.service.domain.service.SimNormalizationService;
import org.springframework.stereotype.Service;

/**
 * SIM数据规范化服务实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
public class SimNormalizationServiceImpl implements SimNormalizationService {

    private static final String CHINA_COUNTRY_CODE = "86";

    @Override
    public SimInfo normalize(SimInfo simInfo) {
        // trim处理
        String iccid = trimAndValidate(simInfo.getIccid(), "ICCID");
        String imsi = trimAndValidate(simInfo.getImsi(), "IMSI");
        String msisdn = trimAndValidate(simInfo.getMsisdn(), "MSISDN");

        // 校验格式
        if (!validateIccid(iccid)) {
            throw new IllegalArgumentException("ICCID格式无效: " + iccid);
        }
        if (!validateImsi(imsi)) {
            throw new IllegalArgumentException("IMSI格式无效: " + imsi);
        }
        if (!validateMsisdn(msisdn)) {
            throw new IllegalArgumentException("MSISDN格式无效: " + msisdn);
        }

        // 规范化MSISDN
        msisdn = normalizeMsisdn(msisdn);

        return SimInfo.builder()
                .iccid(iccid)
                .imsi(imsi)
                .msisdn(msisdn)
                .sourceMno(simInfo.getSourceMno())
                .sourceType(simInfo.getSourceType())
                .sourceRef(simInfo.getSourceRef())
                .simStatus(simInfo.getSimStatus())
                .bindingStatus(simInfo.getBindingStatus())
                .realnameStatus(simInfo.getRealnameStatus())
                .smsStatus(simInfo.getSmsStatus())
                .dataStatus(simInfo.getDataStatus())
                .voiceStatus(simInfo.getVoiceStatus())
                .build();
    }

    @Override
    public boolean validateIccid(String iccid) {
        if (iccid == null || iccid.isEmpty()) {
            return false;
        }
        // 数字字符串，建议长度=20
        return iccid.matches("\\d{19,20}");
    }

    @Override
    public boolean validateImsi(String imsi) {
        if (imsi == null || imsi.isEmpty()) {
            return false;
        }
        // 数字字符串，建议长度=15
        return imsi.matches("\\d{15}");
    }

    @Override
    public boolean validateMsisdn(String msisdn) {
        if (msisdn == null || msisdn.isEmpty()) {
            return false;
        }
        // 数字字符串，长度=11 或 13（含 86）
        return msisdn.matches("\\d{11}") || msisdn.matches("\\d{13}");
    }

    @Override
    public String normalizeMsisdn(String msisdn) {
        if (msisdn == null || msisdn.isEmpty()) {
            return msisdn;
        }
        // 以 86 开头保持不变，否则拼接 86
        if (msisdn.startsWith(CHINA_COUNTRY_CODE)) {
            return msisdn;
        }
        return CHINA_COUNTRY_CODE + msisdn;
    }

    /**
     * trim并校验非空
     *
     * @param value     待处理的值
     * @param fieldName 字段名
     * @return trim后的值
     * @throws IllegalArgumentException 如果trim后为空
     */
    private String trimAndValidate(String value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return trimmed;
    }
}
