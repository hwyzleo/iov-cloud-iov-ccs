package net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SIM卡领域实体
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sim {

    /**
     * 主键
     */
    private Long id;

    /**
     * 集成电路卡识别码
     */
    private String iccid;

    /**
     * 国际移动用户识别号
     */
    private String imsi;

    /**
     * 手机号
     */
    private String msisdn;

    /**
     * 运营商编码
     */
    private String mnoCode;

    /**
     * SIM卡状态：1-测试，2-库存，3-激活
     */
    private Integer simState;

    /**
     * 短信能力：0-关闭，1-开启
     */
    private Integer smsAbility;

    /**
     * 数据能力：0-关闭，1-开启
     */
    private Integer dataAbility;

    /**
     * 语音能力：0-关闭，1-开启
     */
    private Integer voiceAbility;

    /**
     * 备注
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 修改时间
     */
    private LocalDateTime modifyTime;

    /**
     * 修改者
     */
    private String modifyBy;

    /**
     * 记录版本
     */
    private Integer rowVersion;

    /**
     * 记录是否有效：0-无效，1-有效
     */
    private Integer rowValid;

    /**
     * 创建新的SIM卡实体
     *
     * @param iccid    ICCID
     * @param imsi     IMSI
     * @param msisdn   手机号
     * @param mnoCode  运营商编码
     * @param createBy 创建者
     * @return SIM卡实体
     */
    public static Sim create(String iccid, String imsi, String msisdn, String mnoCode, String createBy) {
        LocalDateTime now = LocalDateTime.now();
        return Sim.builder()
                .iccid(iccid)
                .imsi(imsi)
                .msisdn(msisdn)
                .mnoCode(mnoCode)
                .simState(1)
                .smsAbility(1)
                .dataAbility(1)
                .voiceAbility(1)
                .createTime(now)
                .createBy(createBy)
                .modifyTime(now)
                .modifyBy(createBy)
                .rowVersion(1)
                .rowValid(1)
                .build();
    }

    /**
     * 状态流转：测试 -> 库存
     */
    public void transitionToStock() {
        if (this.simState != 1) {
            throw new IllegalStateException("只有测试状态的SIM卡才能流转到库存状态");
        }
        this.simState = 2;
        this.modifyTime = LocalDateTime.now();
    }

    /**
     * 状态流转：库存 -> 激活
     */
    public void transitionToActive() {
        if (this.simState != 2) {
            throw new IllegalStateException("只有库存状态的SIM卡才能流转到激活状态");
        }
        this.simState = 3;
        this.modifyTime = LocalDateTime.now();
    }

    /**
     * 更新能力
     *
     * @param smsAbility   短信能力
     * @param dataAbility  数据能力
     * @param voiceAbility 语音能力
     */
    public void updateAbilities(Integer smsAbility, Integer dataAbility, Integer voiceAbility) {
        if (smsAbility != null) {
            this.smsAbility = smsAbility;
        }
        if (dataAbility != null) {
            this.dataAbility = dataAbility;
        }
        if (voiceAbility != null) {
            this.voiceAbility = voiceAbility;
        }
        this.modifyTime = LocalDateTime.now();
    }
}
