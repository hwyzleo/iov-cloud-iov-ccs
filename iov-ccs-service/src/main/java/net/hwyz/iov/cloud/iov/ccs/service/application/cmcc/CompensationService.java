package net.hwyz.iov.cloud.iov.ccs.service.application.cmcc;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 运维补偿服务接口
 * <p>
 * 提供重跑/扫描能力，支持按fileId/日期范围重跑
 *
 * @author hwyz_leo
 */
public interface CompensationService {

    /**
     * 按fileId重跑CMCC文件处理
     * <p>
     * 只推进不回退，适用于手动补偿场景
     *
     * @param fileId 文件ID
     * @return 是否成功触发重跑
     */
    boolean rerunByFileId(String fileId);

    /**
     * 按日期范围重跑CMCC文件请求
     * <p>
     * 重新发起指定日期范围的文件请求
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 重跑的记录数
     */
    int rerunByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 扫描并重试候选表中PARSED/FAILED的记录
     * <p>
     * 对于retry_count未超限的批次继续推进
     *
     * @return 重试的记录数
     */
    int scanAndRetryCandidates();

    /**
     * 获取待重试的CMCC记录列表
     *
     * @return 待重试记录的fileId列表
     */
    List<String> listPendingRetryFileIds();

    /**
     * 获取候选表中待处理的记录统计
     *
     * @return 统计信息 [batchNo, pendingCount, failedCount]
     */
    List<Object[]> getCandidateStatistics();
}
