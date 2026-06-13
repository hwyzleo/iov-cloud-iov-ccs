package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.po.SimImportCandidatePo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SIM导入候选表 DAO
 *
 * @author hwyz_leo
 */
@Mapper
public interface SimImportCandidateMapper extends BaseDao<SimImportCandidatePo, Long> {

    /**
     * 根据批次类型、批次号和ICCID查询记录
     *
     * @param batchType 批次类型
     * @param batchNo   批次号
     * @param iccid     ICCID
     * @return 候选记录
     */
    SimImportCandidatePo selectByBatchAndIccid(@Param("batchType") String batchType,
                                                @Param("batchNo") String batchNo,
                                                @Param("iccid") String iccid);

    /**
     * 根据批次类型、批次号和ICCID判断是否存在
     *
     * @param batchType 批次类型
     * @param batchNo   批次号
     * @param iccid     ICCID
     * @return 是否存在
     */
    boolean existsByBatchAndIccid(@Param("batchType") String batchType,
                                  @Param("batchNo") String batchNo,
                                  @Param("iccid") String iccid);

    /**
     * 根据批次号查询待入库的记录
     *
     * @param batchNo 批次号
     * @return 待入库记录列表
     */
    List<SimImportCandidatePo> selectPendingByBatchNo(@Param("batchNo") String batchNo);

    /**
     * 更新入库状态
     *
     * @param id          记录ID
     * @param storeStatus 入库状态
     * @param failureReason 失败原因（可选）
     * @return 更新行数
     */
    int updateStoreStatus(@Param("id") Long id,
                          @Param("storeStatus") String storeStatus,
                          @Param("failureReason") String failureReason);
}
