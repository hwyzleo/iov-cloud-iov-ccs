package net.hwyz.iov.cloud.iov.ccs.service.domain.repository;

import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.CmccFileRequestRecord;

/**
 * CMCC文件请求记录仓储接口
 *
 * @author hwyz_leo
 */
public interface CmccFileRequestRecordRepository {

    /**
     * 根据fileId查询记录
     *
     * @param fileId CMCC fileId
     * @return 文件请求记录
     */
    CmccFileRequestRecord getByFileId(String fileId);

    /**
     * 根据fileId判断是否存在
     *
     * @param fileId CMCC fileId
     * @return 是否存在
     */
    boolean existsByFileId(String fileId);

    /**
     * 保存文件请求记录
     *
     * @param record 文件请求记录
     * @return 影响行数
     */
    int save(CmccFileRequestRecord record);

    /**
     * 更新文件请求记录
     *
     * @param record 文件请求记录
     * @return 影响行数
     */
    int update(CmccFileRequestRecord record);
}
