package net.hwyz.iov.cloud.iov.ccs.service.adapter.web.controller;

import net.hwyz.iov.cloud.iov.ccs.service.application.cmcc.CompensationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("运维补偿接口测试")
class CompensationControllerTest {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Mock
    private CompensationService compensationService;

    @InjectMocks
    private CompensationController compensationController;

    // ========== rerunByFileId ==========

    @Test
    @DisplayName("按fileId重跑 - 成功返回200")
    void rerunByFileId_success() {
        when(compensationService.rerunByFileId("file_001")).thenReturn(true);

        var result = compensationController.rerunByFileId("file_001");

        assertEquals(200, result.getStatusCode().value());
        assertTrue((Boolean) result.getBody().get("success"));
    }

    @Test
    @DisplayName("按fileId重跑 - 失败返回400")
    void rerunByFileId_failure() {
        when(compensationService.rerunByFileId("file_001")).thenReturn(false);

        var result = compensationController.rerunByFileId("file_001");

        assertEquals(400, result.getStatusCode().value());
        assertFalse((Boolean) result.getBody().get("success"));
    }

    // ========== rerunByDateRange ==========

    @Test
    @DisplayName("按日期范围重跑 - 正常日期返回200")
    void rerunByDateRange_success() {
        when(compensationService.rerunByDateRange(any(), any())).thenReturn(3);

        var result = compensationController.rerunByDateRange(
                "2024-01-01 00:00:00", "2024-03-31 23:59:59");

        assertEquals(200, result.getStatusCode().value());
        assertTrue((Boolean) result.getBody().get("success"));
        assertEquals(3, result.getBody().get("count"));
    }

    @Test
    @DisplayName("按日期范围重跑 - 无效日期返回400")
    void rerunByDateRange_invalidDate() {
        var result = compensationController.rerunByDateRange("invalid", "invalid");

        assertEquals(400, result.getStatusCode().value());
        assertFalse((Boolean) result.getBody().get("success"));
    }

    // ========== scanAndRetryCandidates ==========

    @Test
    @DisplayName("扫描重试候选表 - 返回扫描结果")
    void scanAndRetryCandidates_success() {
        when(compensationService.scanAndRetryCandidates()).thenReturn(5);

        var result = compensationController.scanAndRetryCandidates();

        assertEquals(200, result.getStatusCode().value());
        assertTrue((Boolean) result.getBody().get("success"));
        assertEquals(5, result.getBody().get("count"));
    }

    // ========== listPendingRetryFileIds ==========

    @Test
    @DisplayName("获取待重试列表 - 返回fileId列表")
    void listPendingRetryFileIds_success() {
        when(compensationService.listPendingRetryFileIds()).thenReturn(List.of("file_001", "file_002"));

        var result = compensationController.listPendingRetryFileIds();

        assertEquals(200, result.getStatusCode().value());
        assertTrue((Boolean) result.getBody().get("success"));
    }

    // ========== getCandidateStatistics ==========

    @Test
    @DisplayName("获取候选统计 - 返回统计信息")
    void getCandidateStatistics_success() {
        when(compensationService.getCandidateStatistics()).thenReturn(List.of());

        var result = compensationController.getCandidateStatistics();

        assertEquals(200, result.getStatusCode().value());
        assertTrue((Boolean) result.getBody().get("success"));
    }
}
