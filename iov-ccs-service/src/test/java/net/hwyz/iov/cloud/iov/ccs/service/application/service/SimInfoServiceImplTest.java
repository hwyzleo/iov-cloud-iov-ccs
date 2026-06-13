package net.hwyz.iov.cloud.iov.ccs.service.application.service;

import net.hwyz.iov.cloud.iov.ccs.api.vo.enums.MnoType;
import net.hwyz.iov.cloud.iov.ccs.service.domain.event.BusinessAlertEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.event.SimStorageEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.SimInfoRepository;
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
@DisplayName("SIM信息服务测试")
class SimInfoServiceImplTest {

    @Mock
    private SimInfoRepository simInfoRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SimInfoServiceImpl simInfoService;

    private SimInfo buildSimInfo(String iccid, String imsi, String msisdn) {
        return SimInfo.builder()
                .iccid(iccid)
                .imsi(imsi)
                .msisdn(msisdn)
                .build();
    }

    // ========== handleStorageEvent ==========

    @Test
    @DisplayName("存储事件 - 空列表跳过处理")
    void handleStorageEvent_emptyList() {
        SimStorageEvent event = SimStorageEvent.builder()
                .batchType(MnoType.CMCC.getCode())
                .batchNo("file_001")
                .sourceMno(MnoType.CMCC.getCode())
                .simInfoList(List.of())
                .build();

        simInfoService.handleStorageEvent(event);

        verify(simInfoRepository, never()).save(any());
    }

    @Test
    @DisplayName("存储事件 - null列表跳过处理")
    void handleStorageEvent_nullList() {
        SimStorageEvent event = SimStorageEvent.builder()
                .batchType(MnoType.CMCC.getCode())
                .batchNo("file_001")
                .sourceMno(MnoType.CMCC.getCode())
                .simInfoList(null)
                .build();

        simInfoService.handleStorageEvent(event);

        verify(simInfoRepository, never()).save(any());
    }

    @Test
    @DisplayName("存储事件 - CMCC新ICCID入库成功")
    void handleStorageEvent_cmccNewIccid() {
        SimInfo simInfo = buildSimInfo("89860123456789012345", "460001234567890", "8613800138000");

        SimStorageEvent event = SimStorageEvent.builder()
                .batchType(MnoType.CMCC.getCode())
                .batchNo("file_001")
                .sourceMno(MnoType.CMCC.getCode())
                .simInfoList(List.of(simInfo))
                .build();

        when(simInfoRepository.getByIccid("89860123456789012345")).thenReturn(null);

        simInfoService.handleStorageEvent(event);

        verify(simInfoRepository).save(simInfo);
    }

    @Test
    @DisplayName("存储事件 - CMCC已存在ICCID幂等跳过")
    void handleStorageEvent_cmccDuplicateSkip() {
        SimInfo existing = buildSimInfo("89860123456789012345", "460001234567890", "8613800138000");
        SimInfo incoming = buildSimInfo("89860123456789012345", "460001234567890", "8613800138000");

        SimStorageEvent event = SimStorageEvent.builder()
                .batchType(MnoType.CMCC.getCode())
                .batchNo("file_001")
                .sourceMno(MnoType.CMCC.getCode())
                .simInfoList(List.of(incoming))
                .build();

        when(simInfoRepository.getByIccid("89860123456789012345")).thenReturn(existing);

        simInfoService.handleStorageEvent(event);

        verify(simInfoRepository, never()).save(any());
    }

    @Test
    @DisplayName("存储事件 - CMCC已存在但数据差异触发告警")
    void handleStorageEvent_cmccDataMismatch() {
        SimInfo existing = buildSimInfo("89860123456789012345", "460001234567890", "8613800138000");
        SimInfo incoming = buildSimInfo("89860123456789012345", "460009999999999", "8613800138000");

        SimStorageEvent event = SimStorageEvent.builder()
                .batchType(MnoType.CMCC.getCode())
                .batchNo("file_001")
                .sourceMno(MnoType.CMCC.getCode())
                .simInfoList(List.of(incoming))
                .build();

        when(simInfoRepository.getByIccid("89860123456789012345")).thenReturn(existing);

        simInfoService.handleStorageEvent(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<BusinessAlertEvent> alertCaptor = ArgumentCaptor.forClass(BusinessAlertEvent.class);
        verify(eventPublisher).publishEvent(alertCaptor.capture());
        assertEquals(BusinessAlertEvent.AlertType.SIM_DATA_MISMATCH, alertCaptor.getValue().getAlertType());
    }

    @Test
    @DisplayName("存储事件 - 保存异常触发告警事件")
    void handleStorageEvent_saveException() {
        SimInfo simInfo = buildSimInfo("89860123456789012345", "460001234567890", "8613800138000");

        SimStorageEvent event = SimStorageEvent.builder()
                .batchType(MnoType.CMCC.getCode())
                .batchNo("file_001")
                .sourceMno(MnoType.CMCC.getCode())
                .simInfoList(List.of(simInfo))
                .build();

        when(simInfoRepository.getByIccid("89860123456789012345")).thenReturn(null);
        doThrow(new RuntimeException("DB error")).when(simInfoRepository).save(any());

        simInfoService.handleStorageEvent(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<BusinessAlertEvent> alertCaptor = ArgumentCaptor.forClass(BusinessAlertEvent.class);
        verify(eventPublisher).publishEvent(alertCaptor.capture());
        assertEquals(BusinessAlertEvent.AlertType.SIM_STORE_FAILED, alertCaptor.getValue().getAlertType());
    }

    // ========== saveSimInfo ==========

    @Test
    @DisplayName("保存SIM - 新ICCID保存成功返回true")
    void saveSimInfo_newIccid() {
        SimInfo simInfo = buildSimInfo("89860123456789012345", "460001234567890", "8613800138000");

        when(simInfoRepository.existsByIccid("89860123456789012345")).thenReturn(false);

        boolean result = simInfoService.saveSimInfo(simInfo);

        assertTrue(result);
        verify(simInfoRepository).save(simInfo);
    }

    @Test
    @DisplayName("保存SIM - 已存在ICCID返回false")
    void saveSimInfo_existingIccid() {
        SimInfo simInfo = buildSimInfo("89860123456789012345", "460001234567890", "8613800138000");

        when(simInfoRepository.existsByIccid("89860123456789012345")).thenReturn(true);

        boolean result = simInfoService.saveSimInfo(simInfo);

        assertFalse(result);
        verify(simInfoRepository, never()).save(any());
    }

    // ========== upsertSimInfo ==========

    @Test
    @DisplayName("Upsert - ICCID不存在则插入")
    void upsertSimInfo_insert() {
        SimInfo simInfo = buildSimInfo("89860123456789012345", "460001234567890", "8613800138000");

        when(simInfoRepository.getByIccid("89860123456789012345")).thenReturn(null);

        simInfoService.upsertSimInfo(simInfo);

        verify(simInfoRepository).save(simInfo);
        verify(simInfoRepository, never()).update(any());
    }

    @Test
    @DisplayName("Upsert - ICCID已存在则更新")
    void upsertSimInfo_update() {
        SimInfo existing = buildSimInfo("89860123456789012345", "460001234567890", "8613800138000");
        existing.setId(1L);

        SimInfo incoming = buildSimInfo("89860123456789012345", "460009999999999", "8613900000000");
        incoming.setSourceMno("CUCC");
        incoming.setSourceType("cucc_push");
        incoming.setSourceRef("batch_001");

        when(simInfoRepository.getByIccid("89860123456789012345")).thenReturn(existing);

        simInfoService.upsertSimInfo(incoming);

        verify(simInfoRepository).update(argThat(updated ->
                updated.getId().equals(1L)
                        && "460009999999999".equals(updated.getImsi())
                        && "8613900000000".equals(updated.getMsisdn())
                        && "CUCC".equals(updated.getSourceMno())
        ));
        verify(simInfoRepository, never()).save(any());
    }
}
