package net.hwyz.iov.cloud.iov.ccs.service.application.cmcc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ccs.api.vo.enums.CandidateParseStatus;
import net.hwyz.iov.cloud.iov.ccs.api.vo.enums.CandidateStoreStatus;
import net.hwyz.iov.cloud.iov.ccs.api.vo.enums.CmccRequestStatus;
import net.hwyz.iov.cloud.iov.ccs.api.vo.enums.MnoType;
import net.hwyz.iov.cloud.iov.ccs.service.domain.event.BusinessAlertEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.event.SimStorageEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.CmccFileRequestRecord;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimImportCandidate;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.CmccFileRequestRecordRepository;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.SimImportCandidateRepository;
import net.hwyz.iov.cloud.iov.ccs.service.domain.service.SimNormalizationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * CMCC文件处理服务实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CmccFileServiceImpl implements CmccFileService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String REQUEST_TYPE = "SIM_EFFECTIVE_REQUEST";
    private static final int DEFAULT_MONTHS_RANGE = 3;

    private final CmccClient cmccClient;
    private final CmccFileRequestRecordRepository cmccFileRequestRecordRepository;
    private final SimImportCandidateRepository simImportCandidateRepository;
    private final SimNormalizationService simNormalizationService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public CmccFileRequestRecord requestFile() {
        log.info("开始CMCC文件请求任务");

        // 获取上次同步记录
        // TODO: 从数据库获取最新的记录
        CmccFileRequestRecord lastRecord = null;

        // 计算日期范围
        LocalDateTime[] dateRange = calculateDateRange(lastRecord);
        LocalDateTime startDate = dateRange[0];
        LocalDateTime endDate = dateRange[1];

        log.info("同步日期范围: {} ~ {}", startDate.format(DATE_FORMATTER), endDate.format(DATE_FORMATTER));

        // 创建请求记录
        CmccFileRequestRecord record = CmccFileRequestRecord.builder()
                .requestStart(startDate)
                .requestEnd(endDate)
                .status(CmccRequestStatus.APPLYING.getCode())
                .retryCount(0)
                .build();

        try {
            // 调用CMCC接口请求文件
            CmccClient.FileRequestResult result = cmccClient.requestFile(REQUEST_TYPE, startDate, endDate, true);

            if (result.success()) {
                record.setFileId(result.fileId());
                record.setTs(result.timestamp());
                record.setEncrypted(1);
                cmccFileRequestRecordRepository.save(record);
                log.info("CMCC文件请求成功: fileId={}", result.fileId());
            } else {
                record.setStatus(CmccRequestStatus.FAILED.getCode());
                record.setFailureStage(CmccRequestStatus.APPLYING.getCode());
                record.setFailureReason(result.errorMessage());
                cmccFileRequestRecordRepository.save(record);
                log.error("CMCC文件请求失败: {}", result.errorMessage());

                // 发布告警事件
                eventPublisher.publishEvent(BusinessAlertEvent.builder()
                        .alertType(BusinessAlertEvent.AlertType.CMCC_FILE_REQUEST_FAILED)
                        .message("CMCC文件请求失败")
                        .detail(result.errorMessage())
                        .happenTime(LocalDateTime.now())
                        .build());
            }
        } catch (Exception e) {
            record.setStatus(CmccRequestStatus.FAILED.getCode());
            record.setFailureStage(CmccRequestStatus.APPLYING.getCode());
            record.setFailureReason(e.getMessage());
            cmccFileRequestRecordRepository.save(record);
            log.error("CMCC文件请求异常", e);

            // 发布告警事件
            eventPublisher.publishEvent(BusinessAlertEvent.builder()
                    .alertType(BusinessAlertEvent.AlertType.CMCC_FILE_REQUEST_FAILED)
                    .message("CMCC文件请求异常")
                    .detail(e.getMessage())
                    .happenTime(LocalDateTime.now())
                    .build());
        }

        return record;
    }

    @Override
    @Transactional
    public void handleCallback(String fileId, boolean successful, String message) {
        log.info("收到CMCC回调: fileId={}, successful={}, message={}", fileId, successful, message);

        // 查询请求记录
        CmccFileRequestRecord record = cmccFileRequestRecordRepository.getByFileId(fileId);
        if (record == null) {
            log.warn("未找到对应的文件请求记录: fileId={}", fileId);
            return;
        }

        // 幂等检查：状态只推进不回退
        if (isTerminalStatus(record.getStatus())) {
            log.info("记录已处于终态，跳过处理: fileId={}, status={}", fileId, record.getStatus());
            return;
        }

        if (!successful) {
            // successful=false：仅记录message，不覆盖STORED终态
            if (!CmccRequestStatus.STORED.getCode().equals(record.getStatus())) {
                record.setFailureReason(message);
                cmccFileRequestRecordRepository.update(record);
            }
            log.info("CMCC回调失败: fileId={}, message={}", fileId, message);
            return;
        }

        // successful=true：触发处理流程
        processFile(fileId);
    }

    @Override
    @Transactional
    public void processFile(String fileId) {
        log.info("开始处理CMCC文件: fileId={}", fileId);

        CmccFileRequestRecord record = cmccFileRequestRecordRepository.getByFileId(fileId);
        if (record == null) {
            log.error("未找到文件请求记录: fileId={}", fileId);
            return;
        }

        // 幂等检查：如果已处于DOWNLOADED/DECRYPTED/PARSED/STORED，直接返回
        if (isAfterDownload(record.getStatus())) {
            log.info("文件已处理过，跳过: fileId={}, status={}", fileId, record.getStatus());
            return;
        }

        try {
            // Step 1: 下载文件
            byte[] fileContent = downloadFile(record);
            record.setStatus(CmccRequestStatus.DOWNLOADED.getCode());
            cmccFileRequestRecordRepository.update(record);

            // Step 2: 解密文件（如果加密）
            byte[] decryptedContent = decryptFile(record, fileContent);
            record.setStatus(CmccRequestStatus.DECRYPTED.getCode());
            cmccFileRequestRecordRepository.update(record);

            // Step 3: 解压ZIP
            String csvContent = unzipFile(decryptedContent);

            // Step 4: 解析CSV
            List<SimInfo> simInfoList = parseCsv(csvContent, fileId);

            // Step 5: 写入候选表
            List<SimImportCandidate> candidates = createCandidates(simInfoList, fileId);
            simImportCandidateRepository.batchSave(candidates);
            record.setStatus(CmccRequestStatus.PARSED.getCode());
            record.setParsedTotal(candidates.size());
            cmccFileRequestRecordRepository.update(record);

            // Step 6: 发布存储事件，触发入库
            eventPublisher.publishEvent(SimStorageEvent.builder()
                    .batchType(MnoType.CMCC.getCode())
                    .batchNo(fileId)
                    .sourceMno(MnoType.CMCC.getCode())
                    .simInfoList(simInfoList)
                    .build());

            // Step 7: 更新统计信息
            updateStatistics(record, candidates);
            record.setStatus(CmccRequestStatus.STORED.getCode());
            cmccFileRequestRecordRepository.update(record);

            log.info("CMCC文件处理完成: fileId={}, total={}, success={}, duplicate={}, failed={}",
                    fileId, record.getParsedTotal(), record.getStoredSuccess(),
                    record.getStoredDuplicate(), record.getStoredFailed());

        } catch (Exception e) {
            record.setStatus(CmccRequestStatus.FAILED.getCode());
            record.setFailureReason(e.getMessage());
            cmccFileRequestRecordRepository.update(record);
            log.error("CMCC文件处理失败: fileId={}", fileId, e);

            // 发布告警事件
            eventPublisher.publishEvent(BusinessAlertEvent.builder()
                    .alertType(BusinessAlertEvent.AlertType.CMCC_FILE_PARSE_FAILED)
                    .refKey(fileId)
                    .message("CMCC文件处理失败")
                    .detail(e.getMessage())
                    .happenTime(LocalDateTime.now())
                    .build());
        }
    }

    @Override
    public LocalDateTime[] calculateDateRange(CmccFileRequestRecord lastRecord) {
        LocalDateTime endDate = LocalDateTime.now().with(LocalTime.of(23, 59, 59)).minusDays(1);

        LocalDateTime startDate;
        if (lastRecord == null) {
            // 无历史记录：默认最近3个月
            startDate = endDate.minusMonths(DEFAULT_MONTHS_RANGE);
        } else if (CmccRequestStatus.STORED.getCode().equals(lastRecord.getStatus())) {
            // 上次成功：从上次结束日期的下一天开始
            startDate = lastRecord.getRequestEnd().plusDays(1);
        } else {
            // 上次失败：重试相同的日期范围
            startDate = lastRecord.getRequestStart();
            endDate = lastRecord.getRequestEnd();
        }

        return new LocalDateTime[]{startDate, endDate};
    }

    /**
     * 下载文件
     */
    private byte[] downloadFile(CmccFileRequestRecord record) {
        log.info("下载CMCC文件: fileId={}", record.getFileId());
        byte[] content = cmccClient.downloadFile(record.getFileId(), record.getTs());
        if (content == null || content.length == 0) {
            throw new RuntimeException("文件下载失败: 内容为空");
        }
        return content;
    }

    /**
     * 解密文件
     */
    private byte[] decryptFile(CmccFileRequestRecord record, byte[] encryptedContent) {
        if (record.getEncrypted() == null || record.getEncrypted() == 0) {
            return encryptedContent;
        }

        log.info("解密CMCC文件: fileId={}", record.getFileId());

        // TODO: 实现真实的解密逻辑
        // key = SHA256(eSecret + timestamp)
        // 使用AES解密

        // Mock实现：直接返回原内容
        return encryptedContent;
    }

    /**
     * 解压ZIP文件
     */
    private String unzipFile(byte[] zipContent) {
        log.info("解压ZIP文件");

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipContent));
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            ZipEntry entry = zis.getNextEntry();
            if (entry == null) {
                throw new RuntimeException("ZIP文件为空");
            }

            byte[] buffer = new byte[4096];
            int len;
            while ((len = zis.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }

            return baos.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("ZIP解压失败", e);
        }
    }

    /**
     * 解析CSV文件
     * <p>
     * CSV格式：MSISDN,IMSI,ICCID（首行为表头，从第二行开始解析）
     */
    private List<SimInfo> parseCsv(String csvContent, String fileId) {
        log.info("解析CSV文件");

        List<SimInfo> simInfoList = new ArrayList<>();
        String[] lines = csvContent.split("\n");

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] fields = line.split(",");
            if (fields.length < 3) {
                log.warn("CSV行格式错误: line={}", i + 1);
                continue;
            }

            String msisdn = fields[0].trim();
            String imsi = fields[1].trim();
            String iccid = fields[2].trim();

            try {
                SimInfo simInfo = SimInfo.builder()
                        .iccid(iccid)
                        .imsi(imsi)
                        .msisdn(msisdn)
                        .sourceMno(MnoType.CMCC.getCode())
                        .sourceType("cmcc_file")
                        .sourceRef(fileId)
                        .simStatus(1)  // TEST
                        .bindingStatus(0)  // UNBOUNDED
                        .realnameStatus(1)  // NO_REAL_NAME
                        .smsStatus(true)
                        .dataStatus(true)
                        .voiceStatus(true)
                        .build();

                // 规范化
                simInfo = simNormalizationService.normalize(simInfo);
                simInfoList.add(simInfo);
            } catch (Exception e) {
                log.warn("SIM数据规范化失败: line={}, error={}", i + 1, e.getMessage());
            }
        }

        log.info("CSV解析完成: total={}", simInfoList.size());
        return simInfoList;
    }

    /**
     * 创建候选记录
     */
    private List<SimImportCandidate> createCandidates(List<SimInfo> simInfoList, String fileId) {
        List<SimImportCandidate> candidates = new ArrayList<>();

        for (SimInfo simInfo : simInfoList) {
            SimImportCandidate candidate = SimImportCandidate.builder()
                    .batchType(MnoType.CMCC.getCode())
                    .batchNo(fileId)
                    .sourceMno(MnoType.CMCC.getCode())
                    .iccid(simInfo.getIccid())
                    .imsi(simInfo.getImsi())
                    .msisdn(simInfo.getMsisdn())
                    .parseStatus(CandidateParseStatus.OK.getCode())
                    .storeStatus(CandidateStoreStatus.PENDING.getCode())
                    .build();
            candidates.add(candidate);
        }

        return candidates;
    }

    /**
     * 更新统计信息
     */
    private void updateStatistics(CmccFileRequestRecord record, List<SimImportCandidate> candidates) {
        // TODO: 从候选表查询实际的入库结果
        record.setParsedTotal(candidates.size());
        record.setStoredSuccess(0);
        record.setStoredDuplicate(0);
        record.setStoredFailed(0);
    }

    /**
     * 判断是否为终态
     */
    private boolean isTerminalStatus(String status) {
        return CmccRequestStatus.STORED.getCode().equals(status)
                || CmccRequestStatus.FAILED.getCode().equals(status);
    }

    /**
     * 判断是否已过下载阶段
     */
    private boolean isAfterDownload(String status) {
        return CmccRequestStatus.DOWNLOADED.getCode().equals(status)
                || CmccRequestStatus.DECRYPTED.getCode().equals(status)
                || CmccRequestStatus.PARSED.getCode().equals(status)
                || CmccRequestStatus.STORED.getCode().equals(status);
    }
}
