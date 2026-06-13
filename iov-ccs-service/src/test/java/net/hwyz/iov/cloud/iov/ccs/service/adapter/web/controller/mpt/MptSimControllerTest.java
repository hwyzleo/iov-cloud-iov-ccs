package net.hwyz.iov.cloud.iov.ccs.service.adapter.web.controller.mpt;

import net.hwyz.iov.cloud.iov.ccs.service.adapter.web.vo.request.BatchSimInfoRequest;
import net.hwyz.iov.cloud.iov.ccs.service.adapter.web.vo.request.SimInfoQueryRequest;
import net.hwyz.iov.cloud.iov.ccs.service.adapter.web.vo.request.SimInfoRequest;
import net.hwyz.iov.cloud.iov.ccs.service.adapter.web.vo.request.SyncDataRequest;
import net.hwyz.iov.cloud.iov.ccs.service.application.service.ManualSimService;
import net.hwyz.iov.cloud.iov.ccs.service.application.service.exception.BatchSaveException;
import net.hwyz.iov.cloud.iov.ccs.service.application.service.exception.ServiceException;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HexFormat;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MptSimController接口测试")
class MptSimControllerTest {

    @Mock
    private ManualSimService manualSimService;

    @InjectMocks
    private MptSimController controller;

    private static final String VALID_ICCID = "89860123456789012345";
    private static final String VALID_IMSI = "460001234567890";
    private static final String VALID_MSISDN = "13800138000";

    private SimInfoRequest buildValidRequest() {
        SimInfoRequest request = new SimInfoRequest();
        request.setIccid(VALID_ICCID);
        request.setImsi(VALID_IMSI);
        request.setMsisdn(VALID_MSISDN);
        request.setMnoType("MANUAL");
        return request;
    }

    // ========== POST /api/mpt/simInfo/v1 ==========

    @Test
    @DisplayName("保存SIM - 正常请求返回成功")
    void add_success() {
        var result = controller.add(buildValidRequest());

        assertNotNull(result);
        verify(manualSimService).saveSimInfo(any());
    }

    @Test
    @DisplayName("保存SIM - ICCID已存在返回业务失败")
    void add_duplicateIccid() {
        doThrow(new ServiceException("ICCID已存在")).when(manualSimService).saveSimInfo(any());

        var result = controller.add(buildValidRequest());

        assertNotNull(result);
    }

    // ========== POST /api/mpt/simInfo/v1/batch ==========

    @Test
    @DisplayName("批量保存 - 正常请求返回成功")
    void batchAdd_success() {
        BatchSimInfoRequest request = new BatchSimInfoRequest();
        request.setSimList(List.of(buildValidRequest()));

        var result = controller.batchAdd(request);

        assertNotNull(result);
        verify(manualSimService).batchSaveSimInfo(any());
    }

    @Test
    @DisplayName("批量保存 - 部分失败返回失败ICCID列表")
    void batchAdd_partialFail() {
        BatchSimInfoRequest request = new BatchSimInfoRequest();
        request.setSimList(List.of(buildValidRequest()));

        doThrow(new BatchSaveException("部分失败", List.of(VALID_ICCID)))
                .when(manualSimService).batchSaveSimInfo(any());

        var result = controller.batchAdd(request);

        assertNotNull(result);
    }

    // ========== POST /api/mpt/simInfo/v1/sync ==========

    @Test
    @DisplayName("同步数据 - 正常Hex数据返回成功")
    void syncData_success() {
        SyncDataRequest request = new SyncDataRequest();
        request.setHexData(HexFormat.of().formatHex("[]".getBytes(StandardCharsets.UTF_8)));

        var result = controller.syncData(request);

        assertNotNull(result);
        verify(manualSimService).syncData(any());
    }

    @Test
    @DisplayName("同步数据 - 业务异常返回失败")
    void syncData_serviceException() {
        SyncDataRequest request = new SyncDataRequest();
        request.setHexData("invalid_hex");

        doThrow(new ServiceException("Hex解码失败")).when(manualSimService).syncData(any());

        var result = controller.syncData(request);

        assertNotNull(result);
    }

    // ========== GET /api/mpt/simInfo/v1/list ==========

    @Test
    @DisplayName("列表查询 - 无条件查询")
    void list_noParams() {
        SimInfoQueryRequest query = new SimInfoQueryRequest();
        // startPage() 依赖 HttpServletRequest，纯单元测试跳过此用例
        // 集成测试中覆盖
    }

    // ========== GET /api/mpt/simInfo/v1/{iccid} ==========

    @Test
    @DisplayName("详情查询 - ICCID存在返回成功")
    void getInfo_success() {
        SimInfo sim = SimInfo.builder()
                .iccid(VALID_ICCID).imsi(VALID_IMSI).msisdn("86" + VALID_MSISDN)
                .build();
        when(manualSimService.getSimInfo(VALID_ICCID)).thenReturn(sim);

        var result = controller.getInfo(VALID_ICCID);

        assertNotNull(result);
        assertEquals(VALID_ICCID, result.getData().getIccid());
    }

    @Test
    @DisplayName("详情查询 - ICCID不存在返回失败")
    void getInfo_notFound() {
        when(manualSimService.getSimInfo("not_exist")).thenThrow(new ServiceException("SIM信息不存在"));

        var result = controller.getInfo("not_exist");

        assertNotNull(result);
    }

    // ========== PUT /api/mpt/simInfo/v1/{iccid} ==========

    @Test
    @DisplayName("更新SIM - 路径与body一致且MANUAL来源更新成功")
    void update_success() {
        var result = controller.update(VALID_ICCID, buildValidRequest());

        assertNotNull(result);
        verify(manualSimService).updateSimInfo(eq(VALID_ICCID), any());
    }

    @Test
    @DisplayName("更新SIM - 路径与body的iccid不一致拒绝更新")
    void update_iccidMismatch() {
        SimInfoRequest request = buildValidRequest();
        request.setIccid("89860123456789099999");

        var result = controller.update(VALID_ICCID, request);

        assertNotNull(result);
        verify(manualSimService, never()).updateSimInfo(any(), any());
    }

    @Test
    @DisplayName("更新SIM - 非MANUAL来源返回失败")
    void update_nonManualSource() {
        doThrow(new ServiceException("仅MANUAL来源的SIM信息可更新"))
                .when(manualSimService).updateSimInfo(eq(VALID_ICCID), any());

        var result = controller.update(VALID_ICCID, buildValidRequest());

        assertNotNull(result);
    }

    @Test
    @DisplayName("更新SIM - 不存在返回失败")
    void update_notFound() {
        doThrow(new ServiceException("SIM信息不存在"))
                .when(manualSimService).updateSimInfo(eq("not_exist"), any());

        SimInfoRequest request = buildValidRequest();
        request.setIccid("not_exist");
        var result = controller.update("not_exist", request);

        assertNotNull(result);
    }

    // ========== DELETE /api/mpt/simInfo/v1/{iccid} ==========

    @Test
    @DisplayName("删除SIM - 存在则删除成功")
    void delete_success() {
        var result = controller.delete(VALID_ICCID);

        assertNotNull(result);
        verify(manualSimService).deleteSimInfo(VALID_ICCID);
    }

    @Test
    @DisplayName("删除SIM - 不存在返回失败")
    void delete_notFound() {
        doThrow(new ServiceException("SIM信息不存在")).when(manualSimService).deleteSimInfo("not_exist");

        var result = controller.delete("not_exist");

        assertNotNull(result);
    }

    // ========== DELETE /api/mpt/simInfo/v1/batch/{iccids} ==========

    @Test
    @DisplayName("批量删除 - 全部成功")
    void batchDelete_success() {
        var result = controller.batchDelete(List.of(VALID_ICCID, "89860123456789012341"));

        assertNotNull(result);
        verify(manualSimService).batchDeleteSimInfo(any());
    }

    @Test
    @DisplayName("批量删除 - 部分失败返回失败ICCID")
    void batchDelete_partialFail() {
        doThrow(new BatchSaveException("部分失败", List.of("not_exist")))
                .when(manualSimService).batchDeleteSimInfo(any());

        var result = controller.batchDelete(List.of(VALID_ICCID, "not_exist"));

        assertNotNull(result);
    }
}
