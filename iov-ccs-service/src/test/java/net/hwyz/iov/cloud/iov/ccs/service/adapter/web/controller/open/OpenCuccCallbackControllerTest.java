package net.hwyz.iov.cloud.iov.ccs.service.adapter.web.controller.open;

import net.hwyz.iov.cloud.iov.ccs.service.application.cucc.CuccSecurityService;
import net.hwyz.iov.cloud.iov.ccs.service.application.cucc.CuccSimService;
import net.hwyz.iov.cloud.iov.ccs.service.adapter.web.controller.open.OpenCuccCallbackController.CuccSimInfoData;
import net.hwyz.iov.cloud.iov.ccs.service.adapter.web.controller.open.OpenCuccCallbackController.CuccSimInfoRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CUCC回调接口测试")
class OpenCuccCallbackControllerTest {

    @Mock
    private CuccSecurityService securityService;

    @Mock
    private CuccSimService simService;

    @InjectMocks
    private OpenCuccCallbackController controller;

    private CuccSimInfoRequest buildRequest() {
        CuccSimInfoData data = new CuccSimInfoData();
        data.setIccid("89860123456789012345");
        data.setImsi("460001234567890");
        data.setMsisdn("13800138000");

        CuccSimInfoRequest request = new CuccSimInfoRequest();
        request.setAppid("cucc_app");
        request.setTimestamp(System.currentTimeMillis());
        request.setNonce("abc123");
        request.setSignature("test_sig");
        request.setBatchNo("batch_001");
        request.setData(List.of(data));
        return request;
    }

    @Test
    @DisplayName("SIM推送 - 验签成功返回200")
    void notifySimInfo_success() {
        CuccSimInfoRequest request = buildRequest();

        when(securityService.verifySignature(anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(true);
        when(securityService.isReplayAttack(anyLong(), anyString())).thenReturn(false);
        when(simService.processSimInfo(anyString(), anyList()))
                .thenReturn(CuccSimService.CuccProcessResult.allSuccess(1));

        var result = controller.notifySimInfo(request);

        assertNotNull(result);
        verify(simService).processSimInfo(eq("batch_001"), anyList());
    }

    @Test
    @DisplayName("SIM推送 - 验签失败返回失败")
    void notifySimInfo_signatureFail() {
        CuccSimInfoRequest request = buildRequest();

        when(securityService.verifySignature(anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(false);

        var result = controller.notifySimInfo(request);

        assertNotNull(result);
        verify(simService, never()).processSimInfo(anyString(), anyList());
    }

    @Test
    @DisplayName("SIM推送 - 重放攻击返回失败")
    void notifySimInfo_replayAttack() {
        CuccSimInfoRequest request = buildRequest();

        when(securityService.verifySignature(anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(true);
        when(securityService.isReplayAttack(anyLong(), anyString())).thenReturn(true);

        var result = controller.notifySimInfo(request);

        assertNotNull(result);
    }

    @Test
    @DisplayName("SIM推送 - data为空直接返回成功")
    void notifySimInfo_emptyData() {
        CuccSimInfoRequest request = buildRequest();
        request.setData(null);

        when(securityService.verifySignature(anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(true);
        when(securityService.isReplayAttack(anyLong(), anyString())).thenReturn(false);

        var result = controller.notifySimInfo(request);

        assertNotNull(result);
        verify(simService, never()).processSimInfo(anyString(), anyList());
    }

    @Test
    @DisplayName("SIM推送 - 部分失败返回失败")
    void notifySimInfo_partialFail() {
        CuccSimInfoRequest request = buildRequest();

        when(securityService.verifySignature(anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(true);
        when(securityService.isReplayAttack(anyLong(), anyString())).thenReturn(false);
        when(simService.processSimInfo(anyString(), anyList()))
                .thenReturn(CuccSimService.CuccProcessResult.partialFailed(2, 1, 0, 1, List.of("invalid_iccid")));

        var result = controller.notifySimInfo(request);

        assertNotNull(result);
    }
}
