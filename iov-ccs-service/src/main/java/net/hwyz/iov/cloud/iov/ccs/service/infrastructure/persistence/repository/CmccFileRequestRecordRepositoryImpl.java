package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.CmccFileRequestRecord;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.CmccFileRequestRecordRepository;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.mapper.CmccFileRequestRecordMapper;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.po.CmccFileRequestRecordPo;
import org.springframework.stereotype.Repository;

/**
 * CMCC文件请求记录仓储实现
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class CmccFileRequestRecordRepositoryImpl implements CmccFileRequestRecordRepository {

    private final CmccFileRequestRecordMapper cmccFileRequestRecordMapper;

    @Override
    public CmccFileRequestRecord getByFileId(String fileId) {
        CmccFileRequestRecordPo po = cmccFileRequestRecordMapper.selectByFileId(fileId);
        return po != null ? convertToEntity(po) : null;
    }

    @Override
    public boolean existsByFileId(String fileId) {
        return cmccFileRequestRecordMapper.existsByFileId(fileId);
    }

    @Override
    public int save(CmccFileRequestRecord record) {
        CmccFileRequestRecordPo po = convertToPo(record);
        return cmccFileRequestRecordMapper.insertPo(po);
    }

    @Override
    public int update(CmccFileRequestRecord record) {
        CmccFileRequestRecordPo po = convertToPo(record);
        return cmccFileRequestRecordMapper.updatePo(po);
    }

    /**
     * PO转实体
     *
     * @param po 持久化对象
     * @return 实体
     */
    private CmccFileRequestRecord convertToEntity(CmccFileRequestRecordPo po) {
        return CmccFileRequestRecord.builder()
                .id(po.getId())
                .fileId(po.getFileId())
                .requestStart(po.getRequestStart())
                .requestEnd(po.getRequestEnd())
                .ts(po.getTs())
                .encrypted(po.getEncrypted())
                .status(po.getStatus())
                .retryCount(po.getRetryCount())
                .failureStage(po.getFailureStage())
                .failureReason(po.getFailureReason())
                .parsedTotal(po.getParsedTotal())
                .storedSuccess(po.getStoredSuccess())
                .storedDuplicate(po.getStoredDuplicate())
                .storedFailed(po.getStoredFailed())
                .createdTime(po.getCreatedTime())
                .updatedTime(po.getUpdatedTime())
                .build();
    }

    /**
     * 实体转PO
     *
     * @param entity 实体
     * @return 持久化对象
     */
    private CmccFileRequestRecordPo convertToPo(CmccFileRequestRecord entity) {
        return CmccFileRequestRecordPo.builder()
                .id(entity.getId())
                .fileId(entity.getFileId())
                .requestStart(entity.getRequestStart())
                .requestEnd(entity.getRequestEnd())
                .ts(entity.getTs())
                .encrypted(entity.getEncrypted())
                .status(entity.getStatus())
                .retryCount(entity.getRetryCount())
                .failureStage(entity.getFailureStage())
                .failureReason(entity.getFailureReason())
                .parsedTotal(entity.getParsedTotal())
                .storedSuccess(entity.getStoredSuccess())
                .storedDuplicate(entity.getStoredDuplicate())
                .storedFailed(entity.getStoredFailed())
                .build();
    }
}
