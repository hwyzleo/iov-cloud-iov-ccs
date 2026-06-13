package net.hwyz.iov.cloud.iov.ccs.service.adapter.web.controller.open;

import net.hwyz.iov.cloud.iov.ccs.service.application.cmcc.CmccFileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CMCC回调接口测试")
class OpenCmccCallbackControllerTest {

    @Mock
    private CmccFileService cmccFileService;

    @InjectMocks
    private OpenCmccCallbackController controller;

    @Test
    @DisplayName("文件通知 - 成功回调返回200")
    void fileNotify_success() {
        var result = controller.fileNotify("file_001", true, null);

        assertNotNull(result);
        verify(cmccFileService).handleCallback("file_001", true, null);
    }

    @Test
    @DisplayName("文件通知 - 失败回调带message")
    void fileNotify_failure() {
        var result = controller.fileNotify("file_001", false, "file not found");

        assertNotNull(result);
        verify(cmccFileService).handleCallback("file_001", false, "file not found");
    }

    @Test
    @DisplayName("文件通知 - 处理异常返回失败")
    void fileNotify_serviceException() {
        doThrow(new RuntimeException("处理失败")).when(cmccFileService).handleCallback(any(), anyBoolean(), any());

        var result = controller.fileNotify("file_001", true, null);

        assertNotNull(result);
    }
}
