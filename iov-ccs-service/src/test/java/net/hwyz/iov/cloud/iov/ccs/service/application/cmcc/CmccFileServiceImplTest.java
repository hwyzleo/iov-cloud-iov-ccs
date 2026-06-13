package net.hwyz.iov.cloud.iov.ccs.service.application.cmcc;

import net.hwyz.iov.cloud.iov.ccs.api.vo.enums.CmccRequestStatus;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.CmccFileRequestRecord;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.CmccFileRequestRecordRepository;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.SimImportCandidateRepository;
import net.hwyz.iov.cloud.iov.ccs.service.domain.service.SimNormalizationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CMCC文件处理服务测试")
class CmccFileServiceImplTest {

    @Mock
    private CmccClient cmccClient;

    @Mock
    private CmccFileRequestRecordRepository cmccFileRequestRecordRepository;

    @Mock
    private SimImportCandidateRepository simImportCandidateRepository;

    @Mock
    private SimNormalizationService simNormalizationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CmccFileServiceImpl cmccFileService;

    // ========== calculateDateRange ==========

    @Test
    @DisplayName("日期范围计算 - 无历史记录默认3个月")
    void calculateDateRange_noHistory() {
        LocalDateTime[] range = cmccFileService.calculateDateRange(null);

        assertNotNull(range);
        assertEquals(2, range.length);
        assertNotNull(range[0]);
        assertNotNull(range[1]);

        // endDate应为昨天23:59:59
        LocalDateTime expectedEnd = LocalDateTime.now().with(LocalTime.of(23, 59, 59)).minusDays(1);
        assertEquals(expectedEnd.toLocalDate(), range[1].toLocalDate());

        // startDate应为endDate前3个月
        assertTrue(range[0].isBefore(range[1]));
        assertEquals(range[1].minusMonths(3).toLocalDate(), range[0].toLocalDate());
    }

    @Test
    @DisplayName("日期范围计算 - 上次成功从结束日期+1天开始")
    void calculateDateRange_lastSuccess() {
        CmccFileRequestRecord lastRecord = CmccFileRequestRecord.builder()
                .status(CmccRequestStatus.STORED.getCode())
                .requestStart(LocalDateTime.of(2024, 1, 1, 0, 0, 0))
                .requestEnd(LocalDateTime.of(2024, 3, 31, 23, 59, 59))
                .build();

        LocalDateTime[] range = cmccFileService.calculateDateRange(lastRecord);

        assertNotNull(range);
        // startDate应为上次endDate的下一天
        assertEquals(LocalDateTime.of(2024, 4, 1, 0, 0, 0), range[0]);
        // endDate应为昨天23:59:59
        LocalDateTime expectedEnd = LocalDateTime.now().with(LocalTime.of(23, 59, 59)).minusDays(1);
        assertEquals(expectedEnd.toLocalDate(), range[1].toLocalDate());
    }

    @Test
    @DisplayName("日期范围计算 - 上次失败重试相同日期范围")
    void calculateDateRange_lastFailed() {
        CmccFileRequestRecord lastRecord = CmccFileRequestRecord.builder()
                .status(CmccRequestStatus.FAILED.getCode())
                .requestStart(LocalDateTime.of(2024, 1, 1, 0, 0, 0))
                .requestEnd(LocalDateTime.of(2024, 3, 31, 23, 59, 59))
                .build();

        LocalDateTime[] range = cmccFileService.calculateDateRange(lastRecord);

        assertNotNull(range);
        // 应重试相同的日期范围
        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0, 0), range[0]);
        assertEquals(LocalDateTime.of(2024, 3, 31, 23, 59, 59), range[1]);
    }

    // ========== handleCallback ==========

    @Test
    @DisplayName("回调处理 - 记录不存在直接返回")
    void handleCallback_recordNotFound() {
        when(cmccFileRequestRecordRepository.getByFileId("file_001")).thenReturn(null);

        // 不应抛异常
        cmccFileService.handleCallback("file_001", true, null);

        verify(cmccFileRequestRecordRepository, never()).update(any());
    }

    @Test
    @DisplayName("回调处理 - 终态记录跳过处理")
    void handleCallback_terminalStatus() {
        CmccFileRequestRecord record = CmccFileRequestRecord.builder()
                .fileId("file_001")
                .status(CmccRequestStatus.STORED.getCode())
                .build();

        when(cmccFileRequestRecordRepository.getByFileId("file_001")).thenReturn(record);

        cmccFileService.handleCallback("file_001", true, null);

        verify(cmccFileRequestRecordRepository, never()).update(any());
    }
}
