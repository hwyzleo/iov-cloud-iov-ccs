package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.po.SimInfoPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * SIM基础信息表 DAO
 *
 * @author hwyz_leo
 */
@Mapper
public interface SimInfoMapper extends BaseDao<SimInfoPo, Long> {

    /**
     * 根据ICCID查询SIM信息
     *
     * @param iccid ICCID
     * @return SIM信息
     */
    SimInfoPo selectByIccid(String iccid);

    /**
     * 根据ICCID判断是否存在
     *
     * @param iccid ICCID
     * @return 是否存在
     */
    boolean existsByIccid(String iccid);
}
