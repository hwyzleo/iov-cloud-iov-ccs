package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.po.CmccFileRequestRecordPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * CMCC文件请求记录表 DAO
 *
 * @author hwyz_leo
 */
@Mapper
public interface CmccFileRequestRecordMapper extends BaseDao<CmccFileRequestRecordPo, Long> {

    /**
     * 根据fileId查询记录
     *
     * @param fileId CMCC fileId
     * @return 文件请求记录
     */
    CmccFileRequestRecordPo selectByFileId(String fileId);

    /**
     * 根据fileId判断是否存在
     *
     * @param fileId CMCC fileId
     * @return 是否存在
     */
    boolean existsByFileId(String fileId);
}
