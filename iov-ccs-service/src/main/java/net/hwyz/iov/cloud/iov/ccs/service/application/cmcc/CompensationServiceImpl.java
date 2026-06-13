package net.hwyz.iov.cloud.iov.ccs.service.application.cmcc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ccs.api.vo.enums.CmccRequestStatus;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.CmccFileRequestRecord;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.CmccFileRequestRecordRepository;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.SimImportCandidateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 运维补偿服务实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompensationServiceImpl implements CompensationService {

    private static final int MAX_RETRY_COUNT = 3;

    private final CmccFileService cmccFileService;
    private final CmccFileRequestRecordRepository cmccFileRequestRecordRepository;
    private final SimImportCandidateRepository simImportCandidateRepository;

    @Override
    @Transactional
    public boolean rerunByFileId(String fileId) {
        logger.info("按fileId重跑CMCC文件处理: fileId={}", fileId);

        CmccFileRequestRecord record = cmccFileRequestRecordRepository.getByFileId(fileId);
        if (record == null) {
            logger.warn("未找到文件请求记录: fileId={}", fileId);
            return false;
        }

        // 只推进不回退：如果已处于终态，跳过
        if (isTerminalStatus(record.getStatus())) {
            logger.info("记录已处于终态，跳过重跑: fileId={}, status={}", fileId, record.getStatus());
            return false;
        }

        // 重置重试次数
        record.setRetryCount(0);
        record.setStatus(CmccRequestStatus.APPLYING.getCode());
        record.setFailureStage(null);
        record.setFailureReason(null);
        cmccFileRequestRecordRepository.update(record);

        // 触发处理流程
        cmccFileService.processFile(fileId);

        logger.info("按fileId重跑完成: fileId={}", fileId);
        return true;
    }

    @Override
    @Transactional
    public int rerunByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("按日期范围重跑CMCC文件请求: startDate={}, endDate={}", startDate, endDate);

        // TODO: 查询指定日期范围内的失败记录
        // 1. 查询状态为FAILED且request_start在日期范围内的记录
        // 2. 重置状态为APPLYING
        // 3. 重新发起文件请求

        // Mock实现
        logger.info("按日期范围重跑完成: 重跑0条记录");
        return 0;
    }

    @Override
    @Transactional
    public int scanAndRetryCandidates() {
        logger.info("扫描并重试候选表中PARSED/FAILED的记录");

        // TODO: 实现候选表扫描逻辑
        // 1. 查询store_status为PENDING且parse_status为OK的记录
        // 2. 对于retry_count < MAX_RETRY_COUNT的记录，尝试重新入库
        // 3. 更新retry_count和store_status

        // Mock实现
        logger.info("候选表扫描完成: 重试0条记录");
        return 0;
    }

    @Override
    public List<String> listPendingRetryFileIds() {
        logger.info("获取待重试的CMCC记录列表");

        // TODO: 查询状态为PARSED/FAILED且retry_count < MAX_RETRY_COUNT的记录
        // 返回fileId列表

        // Mock实现
        return new ArrayList<>();
    }

    @Override
    public List<Object[]> getCandidateStatistics() {
        logger.info("获取候选表统计信息");

        // TODO: 按batchNo分组统计pending和failed数量
        // SELECT batch_no, 
        //        SUM(CASE WHEN store_status = 'PENDING' THEN 1 ELSE 0 END) as pending_count,
        //        SUM(CASE WHEN store_status = 'FAILED' THEN 1 ELSE 0 END) as failed_count
        // FROM tb_sim_import_candidate
        // GROUP BY batch_no
        // HAVING pending_count > 0 OR failed_count > 0

        // Mock实现
        return new ArrayList<>();
    }

    /**
     * 判断是否为终态
     */
    private boolean isTerminalStatus(String status) {
        return CmccRequestStatus.STORED.getCode().equals(status)
                || CmccRequestStatus.FAILED.getCode().equals(status);
    }
}
