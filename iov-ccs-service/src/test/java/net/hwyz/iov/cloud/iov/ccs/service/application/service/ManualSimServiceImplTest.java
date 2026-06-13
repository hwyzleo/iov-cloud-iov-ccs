package net.hwyz.iov.cloud.iov.ccs.service.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.iov.ccs.service.application.service.exception.BatchSaveException;
import net.hwyz.iov.cloud.iov.ccs.service.application.service.exception.ServiceException;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.SimInfoRepository;
import net.hwyz.iov.cloud.iov.ccs.service.domain.service.SimNormalizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("手动录入SIM服务测试")
class ManualSimServiceImplTest {

    @Mock
    private SimInfoRepository simInfoRepository;

    @Mock
    private SimNormalizationService simNormalizationService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ManualSimServiceImpl manualSimService;

    private SimInfo buildNormalizedSim(String iccid) {
        return SimInfo.builder()
                .iccid(iccid)
                .imsi("460001234567890")
                .msisdn("8613800138000")
                .build();
    }

    // ========== saveSimInfo ==========

    @Test
    @DisplayName("保存SIM - 新ICCID保存成功")
    void saveSimInfo_success() {
        SimInfo input = SimInfo.builder()
                .iccid("89860123456789012345")
                .imsi("460001234567890")
                .msisdn("13800138000")
                .build();

        SimInfo normalized = buildNormalizedSim("89860123456789012345");

        when(simNormalizationService.normalize(input)).thenReturn(normalized);
        when(simInfoRepository.existsByIccid("89860123456789012345")).thenReturn(false);

        manualSimService.saveSimInfo(input);

        verify(simInfoRepository).save(argThat(saved ->
                saved.getSimStatus() == 1
                        && saved.getBindingStatus() == 0
                        && saved.getRealnameStatus() == 1
                        && Boolean.TRUE.equals(saved.getSmsStatus())
                        && Boolean.TRUE.equals(saved.getDataStatus())
                        && Boolean.TRUE.equals(saved.getVoiceStatus())
                        && "UNKNOWN".equals(saved.getSourceMno())
                        && "manual_save".equals(saved.getSourceType())
        ));
    }

    @Test
    @DisplayName("保存SIM - ICCID已存在抛异常")
    void saveSimInfo_duplicateIccid() {
        SimInfo input = SimInfo.builder()
                .iccid("89860123456789012345")
                .imsi("460001234567890")
                .msisdn("13800138000")
                .build();

        SimInfo normalized = buildNormalizedSim("89860123456789012345");

        when(simNormalizationService.normalize(input)).thenReturn(normalized);
        when(simInfoRepository.existsByIccid("89860123456789012345")).thenReturn(true);

        assertThrows(ServiceException.class, () -> manualSimService.saveSimInfo(input));
        verify(simInfoRepository, never()).save(any());
    }

    @Test
    @DisplayName("保存SIM - 规范化失败不入库")
    void saveSimInfo_normalizationFailed() {
        SimInfo input = SimInfo.builder()
                .iccid("invalid")
                .imsi("460001234567890")
                .msisdn("13800138000")
                .build();

        when(simNormalizationService.normalize(input)).thenThrow(new IllegalArgumentException("ICCID格式无效"));

        assertThrows(IllegalArgumentException.class, () -> manualSimService.saveSimInfo(input));
        verify(simInfoRepository, never()).save(any());
    }

    // ========== batchSaveSimInfo ==========

    @Test
    @DisplayName("批量保存 - 全部成功不抛异常")
    void batchSaveSimInfo_allSuccess() {
        SimInfo sim1 = SimInfo.builder().iccid("89860123456789012341").imsi("460001234567891").msisdn("13800138001").build();
        SimInfo sim2 = SimInfo.builder().iccid("89860123456789012342").imsi("460001234567892").msisdn("13800138002").build();

        SimInfo norm1 = buildNormalizedSim("89860123456789012341");
        SimInfo norm2 = buildNormalizedSim("89860123456789012342");

        when(simNormalizationService.normalize(sim1)).thenReturn(norm1);
        when(simNormalizationService.normalize(sim2)).thenReturn(norm2);
        when(simInfoRepository.existsByIccid(any())).thenReturn(false);

        manualSimService.batchSaveSimInfo(List.of(sim1, sim2));

        verify(simInfoRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("批量保存 - 部分失败抛BatchSaveException")
    void batchSaveSimInfo_partialFail() {
        SimInfo sim1 = SimInfo.builder().iccid("89860123456789012341").imsi("460001234567891").msisdn("13800138001").build();
        SimInfo sim2 = SimInfo.builder().iccid("89860123456789012342").imsi("460001234567892").msisdn("13800138002").build();

        SimInfo norm1 = buildNormalizedSim("89860123456789012341");
        SimInfo norm2 = buildNormalizedSim("89860123456789012342");

        when(simNormalizationService.normalize(sim1)).thenReturn(norm1);
        when(simNormalizationService.normalize(sim2)).thenReturn(norm2);
        when(simInfoRepository.existsByIccid("89860123456789012341")).thenReturn(false);
        when(simInfoRepository.existsByIccid("89860123456789012342")).thenReturn(true);

        BatchSaveException ex = assertThrows(BatchSaveException.class,
                () -> manualSimService.batchSaveSimInfo(List.of(sim1, sim2)));

        assertEquals(1, ex.getFailedIccids().size());
        assertTrue(ex.getFailedIccids().contains("89860123456789012342"));
    }

    @Test
    @DisplayName("批量保存 - 空列表不操作")
    void batchSaveSimInfo_emptyList() {
        manualSimService.batchSaveSimInfo(List.of());
        verify(simInfoRepository, never()).save(any());
    }

    @Test
    @DisplayName("批量保存 - null列表不操作")
    void batchSaveSimInfo_nullList() {
        manualSimService.batchSaveSimInfo(null);
        verify(simInfoRepository, never()).save(any());
    }

    // ========== syncData ==========

    @Test
    @DisplayName("同步数据 - 正常Hex数据")
    void syncData_success() {
        String json = "[{\"iccid\":\"89860123456789012345\",\"imsi\":\"460001234567890\",\"msisdn\":\"13800138000\"}]";
        String hexData = HexFormat.of().formatHex(json.getBytes(StandardCharsets.UTF_8));

        SimInfo normalized = buildNormalizedSim("89860123456789012345");
        when(simNormalizationService.normalize(any())).thenReturn(normalized);

        manualSimService.syncData(hexData);

        verify(simInfoRepository).upsertSimInfo(argThat(saved ->
                saved.getSimStatus() == 1
                        && saved.getBindingStatus() == 0
                        && saved.getRealnameStatus() == 1
                        && Boolean.TRUE.equals(saved.getSmsStatus())
                        && Boolean.TRUE.equals(saved.getDataStatus())
                        && Boolean.TRUE.equals(saved.getVoiceStatus())
        ));
    }

    @Test
    @DisplayName("同步数据 - null数据抛异常")
    void syncData_nullData() {
        assertThrows(ServiceException.class, () -> manualSimService.syncData(null));
    }

    @Test
    @DisplayName("同步数据 - 空字符串抛异常")
    void syncData_emptyData() {
        assertThrows(ServiceException.class, () -> manualSimService.syncData(""));
    }

    @Test
    @DisplayName("同步数据 - 无效Hex数据抛异常")
    void syncData_invalidHex() {
        assertThrows(Exception.class, () -> manualSimService.syncData("not_valid_hex"));
    }

    @Test
    @DisplayName("同步数据 - 空JSON数组不操作")
    void syncData_emptyJsonArray() {
        String json = "[]";
        String hexData = HexFormat.of().formatHex(json.getBytes(StandardCharsets.UTF_8));

        manualSimService.syncData(hexData);

        verify(simInfoRepository, never()).upsertSimInfo(any());
    }

    // ========== listSimInfo ==========

    @Test
    @DisplayName("列表查询 - 委托Repository条件查询")
    void listSimInfo_success() {
        Map<String, Object> params = Map.of("iccid", "8986");
        SimInfo sim = buildNormalizedSim("89860123456789012345");
        when(simInfoRepository.listByCondition(params)).thenReturn(List.of(sim));

        List<SimInfo> result = manualSimService.listSimInfo(params);

        assertEquals(1, result.size());
        verify(simInfoRepository).listByCondition(params);
    }

    @Test
    @DisplayName("列表查询 - 空条件返回全部")
    void listSimInfo_emptyParams() {
        when(simInfoRepository.listByCondition(Map.of())).thenReturn(List.of());

        List<SimInfo> result = manualSimService.listSimInfo(Map.of());

        assertTrue(result.isEmpty());
    }

    // ========== getSimInfo ==========

    @Test
    @DisplayName("详情查询 - ICCID存在返回SIM信息")
    void getSimInfo_success() {
        SimInfo sim = buildNormalizedSim("89860123456789012345");
        when(simInfoRepository.getByIccid("89860123456789012345")).thenReturn(sim);

        SimInfo result = manualSimService.getSimInfo("89860123456789012345");

        assertNotNull(result);
        assertEquals("89860123456789012345", result.getIccid());
    }

    @Test
    @DisplayName("详情查询 - ICCID不存在抛异常")
    void getSimInfo_notFound() {
        when(simInfoRepository.getByIccid("not_exist")).thenReturn(null);

        assertThrows(ServiceException.class, () -> manualSimService.getSimInfo("not_exist"));
    }

    // ========== updateSimInfo ==========

    @Test
    @DisplayName("更新SIM - 手动来源更新成功")
    void updateSimInfo_success() {
        SimInfo existing = SimInfo.builder()
                .id(1L)
                .iccid("89860123456789012345")
                .imsi("460001234567890")
                .msisdn("13800138000")
                .sourceMno("UNKNOWN")
                .sourceType("manual_save")
                .simStatus(1)
                .bindingStatus(0)
                .realnameStatus(1)
                .smsStatus(true)
                .dataStatus(true)
                .voiceStatus(true)
                .build();
        when(simInfoRepository.getByIccid("89860123456789012345")).thenReturn(existing);

        SimInfo update = SimInfo.builder()
                .imsi("460009876543210")
                .msisdn("13900139000")
                .build();

        manualSimService.updateSimInfo("89860123456789012345", update);

        verify(simInfoRepository).update(argThat(updated ->
                "460009876543210".equals(updated.getImsi())
                        && "13900139000".equals(updated.getMsisdn())
                        && "89860123456789012345".equals(updated.getIccid())
        ));
    }

    @Test
    @DisplayName("更新SIM - 不存在抛异常")
    void updateSimInfo_notFound() {
        when(simInfoRepository.getByIccid("not_exist")).thenReturn(null);

        assertThrows(ServiceException.class,
                () -> manualSimService.updateSimInfo("not_exist", SimInfo.builder().build()));
        verify(simInfoRepository, never()).update(any());
    }

    @Test
    @DisplayName("更新SIM - 非手动/同步来源拒绝更新")
    void updateSimInfo_nonManualSource() {
        SimInfo existing = SimInfo.builder()
                .id(1L)
                .iccid("89860123456789012345")
                .sourceMno("CMCC")
                .sourceType("cmcc_file")
                .build();
        when(simInfoRepository.getByIccid("89860123456789012345")).thenReturn(existing);

        assertThrows(ServiceException.class,
                () -> manualSimService.updateSimInfo("89860123456789012345", SimInfo.builder().build()));
        verify(simInfoRepository, never()).update(any());
    }

    // ========== deleteSimInfo ==========

    @Test
    @DisplayName("删除SIM - 存在则物理删除")
    void deleteSimInfo_success() {
        SimInfo existing = SimInfo.builder()
                .id(1L)
                .iccid("89860123456789012345")
                .sourceMno("UNKNOWN")
                .build();
        when(simInfoRepository.getByIccid("89860123456789012345")).thenReturn(existing);

        manualSimService.deleteSimInfo("89860123456789012345");

        verify(simInfoRepository).deleteById(1L);
    }

    @Test
    @DisplayName("删除SIM - 不存在抛异常")
    void deleteSimInfo_notFound() {
        when(simInfoRepository.getByIccid("not_exist")).thenReturn(null);

        assertThrows(ServiceException.class, () -> manualSimService.deleteSimInfo("not_exist"));
        verify(simInfoRepository, never()).deleteById(any());
    }

    // ========== batchDeleteSimInfo ==========

    @Test
    @DisplayName("批量删除 - 全部成功不抛异常")
    void batchDeleteSimInfo_allSuccess() {
        SimInfo sim1 = SimInfo.builder().id(1L).iccid("89860123456789012341").sourceMno("UNKNOWN").build();
        SimInfo sim2 = SimInfo.builder().id(2L).iccid("89860123456789012342").sourceMno("UNKNOWN").build();
        when(simInfoRepository.getByIccid("89860123456789012341")).thenReturn(sim1);
        when(simInfoRepository.getByIccid("89860123456789012342")).thenReturn(sim2);

        manualSimService.batchDeleteSimInfo(List.of("89860123456789012341", "89860123456789012342"));

        verify(simInfoRepository).deleteById(1L);
        verify(simInfoRepository).deleteById(2L);
    }

    @Test
    @DisplayName("批量删除 - 部分失败抛BatchSaveException")
    void batchDeleteSimInfo_partialFail() {
        SimInfo sim1 = SimInfo.builder().id(1L).iccid("89860123456789012341").sourceMno("UNKNOWN").build();
        when(simInfoRepository.getByIccid("89860123456789012341")).thenReturn(sim1);
        when(simInfoRepository.getByIccid("not_exist")).thenReturn(null);

        BatchSaveException ex = assertThrows(BatchSaveException.class,
                () -> manualSimService.batchDeleteSimInfo(List.of("89860123456789012341", "not_exist")));

        assertEquals(1, ex.getFailedIccids().size());
        assertTrue(ex.getFailedIccids().contains("not_exist"));
        verify(simInfoRepository).deleteById(1L);
    }

    @Test
    @DisplayName("批量删除 - 空列表不操作")
    void batchDeleteSimInfo_emptyList() {
        manualSimService.batchDeleteSimInfo(List.of());
        verify(simInfoRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("批量删除 - null列表不操作")
    void batchDeleteSimInfo_nullList() {
        manualSimService.batchDeleteSimInfo(null);
        verify(simInfoRepository, never()).deleteById(any());
    }
}
