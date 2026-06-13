package net.hwyz.iov.cloud.iov.ccs.service.adapter.web.controller.open;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.iov.ccs.service.application.cmcc.CmccFileService;
import org.springframework.web.bind.annotation.*;

/**
 * CMCC回调接口（开放平台）
 * <p>
 * 接收中国移动的文件就绪回调通知
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/open/cmcc/callback")
public class OpenCmccCallbackController {

    private final CmccFileService cmccFileService;

    /**
     * 文件就绪通知回调
     * <p>
     * CMCC在文件准备好后调用此接口通知我方系统
     *
     * @param fileId     文件ID
     * @param successful 是否成功
     * @param message    错误信息（可选）
     * @return 响应
     */
    @PostMapping("/file/notify")
    public ApiResponse<Void> fileNotify(
            @RequestParam("fileId") String fileId,
            @RequestParam("successful") boolean successful,
            @RequestParam(value = "message", required = false) String message) {

        log.info("收到CMCC文件通知: fileId={}, successful={}, message={}", fileId, successful, message);

        try {
            cmccFileService.handleCallback(fileId, successful, message);
            return ApiResponse.ok();
        } catch (Exception e) {
            log.error("处理CMCC回调失败: fileId={}", fileId, e);
            return ApiResponse.fail("处理失败: " + e.getMessage());
        }
    }
}
