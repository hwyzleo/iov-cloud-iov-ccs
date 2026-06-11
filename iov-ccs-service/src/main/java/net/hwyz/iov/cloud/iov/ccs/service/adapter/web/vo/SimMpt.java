package net.hwyz.iov.cloud.iov.ccs.service.adapter.web.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SIM卡管理后台视图对象
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimMpt {

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
}
