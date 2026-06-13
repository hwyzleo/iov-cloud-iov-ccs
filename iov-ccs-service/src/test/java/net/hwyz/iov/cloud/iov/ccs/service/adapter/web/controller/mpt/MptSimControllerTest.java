package net.hwyz.iov.cloud.iov.ccs.service.adapter.web.controller.mpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.iov.ccs.service.adapter.web.vo.request.BatchSimInfoRequest;
import net.hwyz.iov.cloud.iov.ccs.service.adapter.web.vo.request.SimInfoRequest;
import net.hwyz.iov.cloud.iov.ccs.service.adapter.web.vo.request.SyncDataRequest;
import net.hwyz.iov.cloud.iov.ccs.service.application.service.ManualSimService;
import net.hwyz.iov.cloud.iov.ccs.service.application.service.exception.BatchSaveException;
import net.hwyz.iov.cloud.iov.ccs.service.application.service.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MptSimController接口测试")
class MptSimControllerTest {

    @Mock
    private ManualSimService manualSimService;

    @InjectMocks
    private MptSimController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========== POST /api/mpt/simInfo/v1 ==========

    @Test
    @DisplayName("保存SIM - 正常请求返回成功")
    void add_success() {
        SimInfoRequest request = new SimInfoRequest();
        request.setIccid("89860123456789012345");
        request.setImsi("460001234567890");
        request.setMsisdn("13800138000");
        request.setMnoType("MANUAL");

        var result = controller.add(request);

        assertNotNull(result);
        verify(manualSimService).saveSimInfo(any());
    }

    @Test
    @DisplayName("保存SIM - ICCID已存在返回业务失败")
    void add_duplicateIccid() {
        SimInfoRequest request = new SimInfoRequest();
        request.setIccid("89860123456789012345");
        request.setImsi("460001234567890");
        request.setMsisdn("13800138000");
        request.setMnoType("MANUAL");

        doThrow(new ServiceException("ICCID已存在")).when(manualSimService).saveSimInfo(any());

        var result = controller.add(request);

        assertNotNull(result);
    }

    // ========== POST /api/mpt/simInfo/v1/batch ==========

    @Test
    @DisplayName("批量保存 - 正常请求返回成功")
    void batchAdd_success() {
        SimInfoRequest simReq = new SimInfoRequest();
        simReq.setIccid("89860123456789012345");
        simReq.setImsi("460001234567890");
        simReq.setMsisdn("13800138000");
        simReq.setMnoType("MANUAL");

        BatchSimInfoRequest request = new BatchSimInfoRequest();
        request.setSimList(List.of(simReq));

        var result = controller.batchAdd(request);

        assertNotNull(result);
        verify(manualSimService).batchSaveSimInfo(any());
    }

    @Test
    @DisplayName("批量保存 - 部分失败返回失败ICCID列表")
    void batchAdd_partialFail() {
        SimInfoRequest simReq = new SimInfoRequest();
        simReq.setIccid("89860123456789012345");
        simReq.setImsi("460001234567890");
        simReq.setMsisdn("13800138000");
        simReq.setMnoType("MANUAL");

        BatchSimInfoRequest request = new BatchSimInfoRequest();
        request.setSimList(List.of(simReq));

        doThrow(new BatchSaveException("部分失败", List.of("89860123456789012345")))
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
}
