package net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SIM卡变更日志领域实体
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimLog {

    /**
     * 主键
     */
    private Long id;

    /**
     * 集成电路卡识别码
     */
    private String iccid;

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
     * 创建变更日志
     *
     * @param sim     SIM卡实体
     * @param remark  备注
     * @return 变更日志
     */
    public static SimLog create(Sim sim, String remark) {
        LocalDateTime now = LocalDateTime.now();
        return SimLog.builder()
                .iccid(sim.getIccid())
                .simState(sim.getSimState())
                .smsAbility(sim.getSmsAbility())
                .dataAbility(sim.getDataAbility())
                .voiceAbility(sim.getVoiceAbility())
                .description(remark)
                .createTime(now)
                .createBy(sim.getModifyBy())
                .modifyTime(now)
                .modifyBy(sim.getModifyBy())
                .rowVersion(1)
                .rowValid(1)
                .build();
    }
}
