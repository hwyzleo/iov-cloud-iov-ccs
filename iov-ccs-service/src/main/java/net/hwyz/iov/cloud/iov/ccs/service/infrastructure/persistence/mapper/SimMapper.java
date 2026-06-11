package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.po.SimPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SIM卡Mapper接口
 *
 * @author hwyz_leo
 */
@Mapper
public interface SimMapper extends BaseMapper<SimPo> {

    /**
     * 根据ICCID查询SIM卡
     *
     * @param iccid ICCID
     * @return SIM卡持久化对象
     */
    SimPo selectByIccid(@Param("iccid") String iccid);

    /**
     * 根据ICCID查询有效SIM卡数量
     *
     * @param iccid ICCID
     * @return 数量
     */
    int countByIccid(@Param("iccid") String iccid);

    /**
     * 根据ICCID逻辑删除SIM卡
     *
     * @param iccid ICCID
     * @return 影响行数
     */
    int deleteByIccid(@Param("iccid") String iccid);
}
