package net.hwyz.iov.cloud.iov.ccs.service.domain.repository;

import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimImportCandidate;

import java.util.List;

/**
 * SIM导入候选仓储接口
 *
 * @author hwyz_leo
 */
public interface SimImportCandidateRepository {

    /**
     * 根据批次类型、批次号和ICCID查询记录
     *
     * @param batchType 批次类型
     * @param batchNo   批次号
     * @param iccid     ICCID
     * @return 候选记录
     */
    SimImportCandidate getByBatchAndIccid(String batchType, String batchNo, String iccid);

    /**
     * 根据批次类型、批次号和ICCID判断是否存在
     *
     * @param batchType 批次类型
     * @param batchNo   批次号
     * @param iccid     ICCID
     * @return 是否存在
     */
    boolean existsByBatchAndIccid(String batchType, String batchNo, String iccid);

    /**
     * 根据批次号查询待入库的记录
     *
     * @param batchNo 批次号
     * @return 待入库记录列表
     */
    List<SimImportCandidate> listPendingByBatchNo(String batchNo);

    /**
     * 保存候选记录
     *
     * @param candidate 候选记录
     * @return 影响行数
     */
    int save(SimImportCandidate candidate);

    /**
     * 批量保存候选记录
     *
     * @param candidates 候选记录列表
     * @return 影响行数
     */
    int batchSave(List<SimImportCandidate> candidates);

    /**
     * 更新入库状态
     *
     * @param id           记录ID
     * @param storeStatus  入库状态
     * @param failureReason 失败原因（可选）
     * @return 影响行数
     */
    int updateStoreStatus(Long id, String storeStatus, String failureReason);
}
