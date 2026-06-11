package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.po.SimLogPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * SIM卡变更日志Mapper接口
 *
 * @author hwyz_leo
 */
@Mapper
public interface SimLogMapper extends BaseMapper<SimLogPo> {

    /**
     * 根据ICCID删除变更日志
     *
     * @param iccid ICCID
     * @return 影响行数
     */
    int deleteByIccid(@Param("iccid") String iccid);
}
