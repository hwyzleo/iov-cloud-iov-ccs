package net.hwyz.iov.cloud.iov.ccs.service.application.cucc;

import net.hwyz.iov.cloud.iov.ccs.api.vo.enums.CandidateParseStatus;
import net.hwyz.iov.cloud.iov.ccs.api.vo.enums.CandidateStoreStatus;
import net.hwyz.iov.cloud.iov.ccs.api.vo.enums.MnoType;
import net.hwyz.iov.cloud.iov.ccs.service.domain.event.SimStorageEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimImportCandidate;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.SimImportCandidateRepository;
import net.hwyz.iov.cloud.iov.ccs.service.domain.service.SimNormalizationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CUCC SIM服务测试")
class CuccSimServiceImplTest {

    @Mock
    private SimNormalizationService simNormalizationService;

    @Mock
    private SimImportCandidateRepository simImportCandidateRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CuccSimServiceImpl cuccSimService;

    private SimInfo buildSimInfo(String iccid, String imsi, String msisdn) {
        return SimInfo.builder().iccid(iccid).imsi(imsi).msisdn(msisdn).build();
    }

    @Test
    @DisplayName("处理SIM信息 - 空列表返回emptyData")
    void processSimInfo_emptyList() {
        CuccSimService.CuccProcessResult result = cuccSimService.processSimInfo("batch_001", List.of());

        assertTrue(result.success());
        assertEquals(0, result.totalCount());
        verify(simImportCandidateRepository, never()).batchSave(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("处理SIM信息 - null列表返回emptyData")
    void processSimInfo_nullList() {
        CuccSimService.CuccProcessResult result = cuccSimService.processSimInfo("batch_001", null);

        assertTrue(result.success());
        assertEquals(0, result.totalCount());
    }

    @Test
    @DisplayName("处理SIM信息 - 全部成功")
    void processSimInfo_allSuccess() {
        SimInfo sim1 = buildSimInfo("89860123456789012341", "460001234567891", "13800138001");
        SimInfo sim2 = buildSimInfo("89860123456789012342", "460001234567892", "13800138002");

        SimInfo norm1 = buildSimInfo("89860123456789012341", "460001234567891", "8613800138001");
        SimInfo norm2 = buildSimInfo("89860123456789012342", "460001234567892", "8613800138002");

        when(simNormalizationService.normalize(sim1)).thenReturn(norm1);
        when(simNormalizationService.normalize(sim2)).thenReturn(norm2);

        CuccSimService.CuccProcessResult result = cuccSimService.processSimInfo("batch_001", List.of(sim1, sim2));

        assertTrue(result.success());
        assertEquals(2, result.totalCount());
        assertEquals(2, result.successCount());
        assertEquals(0, result.failedCount());

        verify(simImportCandidateRepository).batchSave(argThat(candidates -> candidates.size() == 2));
        verify(eventPublisher).publishEvent(any(SimStorageEvent.class));
    }

    @Test
    @DisplayName("处理SIM信息 - 部分规范化失败")
    void processSimInfo_partialNormalizationFail() {
        SimInfo sim1 = buildSimInfo("89860123456789012341", "460001234567891", "13800138001");
        SimInfo sim2 = buildSimInfo("invalid", "460001234567892", "13800138002");

        SimInfo norm1 = buildSimInfo("89860123456789012341", "460001234567891", "8613800138001");

        when(simNormalizationService.normalize(sim1)).thenReturn(norm1);
        when(simNormalizationService.normalize(sim2)).thenThrow(new IllegalArgumentException("ICCID格式无效"));

        CuccSimService.CuccProcessResult result = cuccSimService.processSimInfo("batch_001", List.of(sim1, sim2));

        assertFalse(result.success());
        assertEquals(2, result.totalCount());
        assertEquals(1, result.successCount());
        assertEquals(1, result.failedCount());
        assertTrue(result.failedIccids().contains("invalid"));

        // 保存了2条候选记录（1条OK + 1条INVALID）
        verify(simImportCandidateRepository).batchSave(argThat(candidates -> {
            long okCount = candidates.stream().filter(c -> CandidateParseStatus.OK.getCode().equals(c.getParseStatus())).count();
            long invalidCount = candidates.stream().filter(c -> CandidateParseStatus.INVALID.getCode().equals(c.getParseStatus())).count();
            return candidates.size() == 2 && okCount == 1 && invalidCount == 1;
        }));

        // 仍然发布存储事件（有效列表不为空）
        verify(eventPublisher).publishEvent(any(SimStorageEvent.class));
    }

    @Test
    @DisplayName("处理SIM信息 - 全部规范化失败不发布事件")
    void processSimInfo_allNormalizationFail() {
        SimInfo sim1 = buildSimInfo("invalid1", "460001234567891", "13800138001");

        when(simNormalizationService.normalize(sim1)).thenThrow(new IllegalArgumentException("ICCID格式无效"));

        CuccSimService.CuccProcessResult result = cuccSimService.processSimInfo("batch_001", List.of(sim1));

        assertFalse(result.success());
        assertEquals(1, result.failedCount());

        // 无效记录也保存为候选
        verify(simImportCandidateRepository).batchSave(argThat(candidates ->
                candidates.size() == 1
                        && CandidateParseStatus.INVALID.getCode().equals(candidates.get(0).getParseStatus())
                        && CandidateStoreStatus.FAILED.getCode().equals(candidates.get(0).getStoreStatus())
        ));

        // 全部失败，不发布存储事件
        verify(eventPublisher, never()).publishEvent(any(SimStorageEvent.class));
    }
}
