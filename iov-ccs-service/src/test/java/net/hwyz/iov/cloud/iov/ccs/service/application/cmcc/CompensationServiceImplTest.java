package net.hwyz.iov.cloud.iov.ccs.service.application.cmcc;

import net.hwyz.iov.cloud.iov.ccs.api.vo.enums.CmccRequestStatus;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.CmccFileRequestRecord;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.CmccFileRequestRecordRepository;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.SimImportCandidateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("运维补偿服务测试")
class CompensationServiceImplTest {

    @Mock
    private CmccFileService cmccFileService;

    @Mock
    private CmccFileRequestRecordRepository cmccFileRequestRecordRepository;

    @Mock
    private SimImportCandidateRepository simImportCandidateRepository;

    @InjectMocks
    private CompensationServiceImpl compensationService;

    // ========== rerunByFileId ==========

    @Test
    @DisplayName("按fileId重跑 - 记录不存在返回false")
    void rerunByFileId_notFound() {
        when(cmccFileRequestRecordRepository.getByFileId("file_001")).thenReturn(null);

        boolean result = compensationService.rerunByFileId("file_001");

        assertFalse(result);
        verify(cmccFileService, never()).processFile(any());
    }

    @Test
    @DisplayName("按fileId重跑 - 记录已终态(STORED)返回false")
    void rerunByFileId_terminalStatusStored() {
        CmccFileRequestRecord record = new CmccFileRequestRecord();
        record.setFileId("file_001");
        record.setStatus(CmccRequestStatus.STORED.getCode());

        when(cmccFileRequestRecordRepository.getByFileId("file_001")).thenReturn(record);

        boolean result = compensationService.rerunByFileId("file_001");

        assertFalse(result);
        verify(cmccFileService, never()).processFile(any());
    }

    @Test
    @DisplayName("按fileId重跑 - 记录已终态(FAILED)返回false")
    void rerunByFileId_terminalStatusFailed() {
        CmccFileRequestRecord record = new CmccFileRequestRecord();
        record.setFileId("file_001");
        record.setStatus(CmccRequestStatus.FAILED.getCode());

        when(cmccFileRequestRecordRepository.getByFileId("file_001")).thenReturn(record);

        boolean result = compensationService.rerunByFileId("file_001");

        assertFalse(result);
        verify(cmccFileService, never()).processFile(any());
    }

    @Test
    @DisplayName("按fileId重跑 - 非终态重置并触发处理")
    void rerunByFileId_success() {
        CmccFileRequestRecord record = new CmccFileRequestRecord();
        record.setFileId("file_001");
        record.setStatus(CmccRequestStatus.DOWNLOADED.getCode());
        record.setRetryCount(3);
        record.setFailureStage("DOWNLOAD");
        record.setFailureReason("timeout");

        when(cmccFileRequestRecordRepository.getByFileId("file_001")).thenReturn(record);

        boolean result = compensationService.rerunByFileId("file_001");

        assertTrue(result);
        assertEquals(0, record.getRetryCount());
        assertEquals(CmccRequestStatus.APPLYING.getCode(), record.getStatus());
        assertNull(record.getFailureStage());
        assertNull(record.getFailureReason());

        verify(cmccFileRequestRecordRepository).update(record);
        verify(cmccFileService).processFile("file_001");
    }

    // ========== scanAndRetryCandidates ==========

    @Test
    @DisplayName("扫描重试 - 当前返回0（TODO实现）")
    void scanAndRetryCandidates_returnsZero() {
        int count = compensationService.scanAndRetryCandidates();
        assertEquals(0, count);
    }

    // ========== listPendingRetryFileIds ==========

    @Test
    @DisplayName("获取待重试列表 - 当前返回空列表（TODO实现）")
    void listPendingRetryFileIds_returnsEmpty() {
        var result = compensationService.listPendingRetryFileIds();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== getCandidateStatistics ==========

    @Test
    @DisplayName("获取候选统计 - 当前返回空列表（TODO实现）")
    void getCandidateStatistics_returnsEmpty() {
        var result = compensationService.getCandidateStatistics();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
