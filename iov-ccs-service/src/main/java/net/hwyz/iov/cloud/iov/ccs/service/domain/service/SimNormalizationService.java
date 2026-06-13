package net.hwyz.iov.cloud.iov.ccs.service.domain.service;

import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;

/**
 * SIM数据规范化服务接口
 * <p>
 * 统一处理ICCID/IMSI/MSISDN的规范化和校验
 *
 * @author hwyz_leo
 */
public interface SimNormalizationService {

    /**
     * 规范化SIM信息
     * <p>
     * 处理规则：
     * 1. trim：ICCID/IMSI/MSISDN 去首尾空白
     * 2. 空值：trim 后为空则判定 INVALID
     * 3. MSISDN 国家码：以 86 开头保持不变，否则拼接 86
     *
     * @param simInfo 待规范化的SIM信息
     * @return 规范化后的SIM信息
     * @throws IllegalArgumentException 如果字段校验失败
     */
    SimInfo normalize(SimInfo simInfo);

    /**
     * 校验ICCID格式
     * <p>
     * 规则：数字字符串，建议长度=20
     *
     * @param iccid ICCID
     * @return 是否有效
     */
    boolean validateIccid(String iccid);

    /**
     * 校验IMSI格式
     * <p>
     * 规则：数字字符串，建议长度=15
     *
     * @param imsi IMSI
     * @return 是否有效
     */
    boolean validateImsi(String imsi);

    /**
     * 校验MSISDN格式
     * <p>
     * 规则：数字字符串，长度=11 或 13（含 86）
     *
     * @param msisdn MSISDN
     * @return 是否有效
     */
    boolean validateMsisdn(String msisdn);

    /**
     * 规范化MSISDN
     * <p>
     * 规则：以 86 开头保持不变，否则拼接 86
     *
     * @param msisdn 原始MSISDN
     * @return 规范化后的MSISDN
     */
    String normalizeMsisdn(String msisdn);
}
