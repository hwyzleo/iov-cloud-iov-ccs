package net.hwyz.iov.cloud.iov.ccs.service.adapter.web.controller.open;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.iov.ccs.service.application.cucc.CuccSecurityService;
import net.hwyz.iov.cloud.iov.ccs.service.application.cucc.CuccSimService;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CUCC回调接口（开放平台）
 * <p>
 * 接收中国联通的SIM信息推送
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/open/cucc/callback")
public class OpenCuccCallbackController {

    private final CuccSecurityService securityService;
    private final CuccSimService simService;

    /**
     * SIM信息推送接口
     * <p>
     * CUCC主动推送SIM卡信息到此接口
     *
     * @param request 请求体
     * @return 响应
     */
    @PostMapping("/notify/simInfo")
    public ApiResponse<Void> notifySimInfo(@RequestBody CuccSimInfoRequest request) {
        log.info("收到CUCC SIM信息推送: appid={}, batchNo={}, dataCount={}",
                request.getAppid(), request.getBatchNo(),
                request.getData() != null ? request.getData().size() : 0);

        // Step 1: 验签
        String body = toJsonString(request);
        if (!securityService.verifySignature(request.getAppid(), request.getTimestamp(),
                request.getNonce(), request.getSignature(), body)) {
            log.warn("CUCC签名验证失败: appid={}", request.getAppid());
            return ApiResponse.fail("签名验证失败");
        }

        // Step 2: 防重放检查
        if (securityService.isReplayAttack(request.getTimestamp(), request.getNonce())) {
            log.warn("CUCC重放攻击检测: appid={}, nonce={}", request.getAppid(), request.getNonce());
            return ApiResponse.fail("重放攻击检测");
        }

        // Step 3: data为空直接返回成功
        if (request.getData() == null || request.getData().isEmpty()) {
            log.info("CUCC推送数据为空，直接返回成功");
            return ApiResponse.ok();
        }

        // Step 4: 转换为SimInfo列表
        List<SimInfo> simInfoList = request.getData().stream()
                .map(this::convertToSimInfo)
                .toList();

        // Step 5: 处理SIM信息
        CuccSimService.CuccProcessResult result = simService.processSimInfo(request.getBatchNo(), simInfoList);

        // Step 6: 返回结果
        if (result.success()) {
            return ApiResponse.ok();
        } else {
            return ApiResponse.fail("部分失败，失败ICCID: " + String.join(", ", result.failedIccids()));
        }
    }

    /**
     * 转换为SimInfo
     */
    private SimInfo convertToSimInfo(CuccSimInfoData data) {
        return SimInfo.builder()
                .iccid(data.getIccid())
                .imsi(data.getImsi())
                .msisdn(data.getMsisdn())
                .build();
    }

    /**
     * 对象转JSON字符串（简化实现）
     */
    private String toJsonString(Object obj) {
        // TODO: 使用Jackson或其他JSON库
        return obj.toString();
    }

    /**
     * CUCC SIM信息推送请求
     */
    @Data
    public static class CuccSimInfoRequest {
        private String appid;
        private long timestamp;
        private String nonce;
        private String signature;
        private String batchNo;
        private List<CuccSimInfoData> data;
    }

    /**
     * CUCC SIM信息数据
     */
    @Data
    public static class CuccSimInfoData {
        private String iccid;
        private String msisdn;
        private String imsi;
        private String ip;
        private String account;
    }
}
