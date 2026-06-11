package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SIM卡变更日志持久化对象
 *
 * @author hwyz_leo
 */
@Data
@TableName("tb_sim_log")
public class SimLogPo {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
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
}
