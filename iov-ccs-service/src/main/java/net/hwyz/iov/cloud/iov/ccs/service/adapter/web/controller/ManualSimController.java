package net.hwyz.iov.cloud.iov.ccs.service.adapter.web.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ccs.service.application.service.ManualSimService;
import net.hwyz.iov.cloud.iov.ccs.service.application.service.exception.BatchSaveException;
import net.hwyz.iov.cloud.iov.ccs.service.application.service.exception.ServiceException;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 手动录入SIM接口
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequestMapping("/api/service/sim/info")
@RequiredArgsConstructor
public class ManualSimController {

    private final ManualSimService manualSimService;

    /**
     * 保存单条SIM信息
     *
     * @param request SIM信息请求
     * @return 响应
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> save(@RequestBody SimInfoRequest request) {
        logger.info("手动保存SIM信息: iccid={}", request.getIccid());

        try {
            SimInfo simInfo = convertToSimInfo(request);
            manualSimService.saveSimInfo(simInfo);

            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "success", true,
                    "msg", "保存成功"
            ));
        } catch (ServiceException e) {
            logger.warn("SIM信息保存失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "code", e.getCode(),
                    "success", false,
                    "msg", e.getMessage()
            ));
        }
    }

    /**
     * 批量保存SIM信息
     *
     * @param request 批量SIM信息请求
     * @return 响应
     */
    @PostMapping("/batch/save")
    public ResponseEntity<Map<String, Object>> batchSave(@RequestBody BatchSimInfoRequest request) {
        logger.info("批量保存SIM信息: count={}", request.getSimList() != null ? request.getSimList().size() : 0);

        try {
            List<SimInfo> simInfoList = request.getSimList().stream()
                    .map(this::convertToSimInfo)
                    .toList();

            manualSimService.batchSaveSimInfo(simInfoList);

            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "success", true,
                    "msg", "批量保存成功"
            ));
        } catch (BatchSaveException e) {
            logger.warn("批量保存部分失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "code", 400,
                    "success", false,
                    "msg", e.getMessage(),
                    "failedIccids", e.getFailedIccids()
            ));
        } catch (ServiceException e) {
            logger.warn("批量保存失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "code", e.getCode(),
                    "success", false,
                    "msg", e.getMessage()
            ));
        }
    }

    /**
     * 同步数据
     *
     * @param request 同步数据请求
     * @return 响应
     */
    @PostMapping("/sync/data")
    public ResponseEntity<Map<String, Object>> syncData(@RequestBody SyncDataRequest request) {
        logger.info("同步SIM数据: hexLength={}", request.getHexData() != null ? request.getHexData().length() : 0);

        try {
            manualSimService.syncData(request.getHexData());

            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "success", true,
                    "msg", "同步成功"
            ));
        } catch (ServiceException e) {
            logger.warn("同步数据失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "code", e.getCode(),
                    "success", false,
                    "msg", e.getMessage()
            ));
        }
    }

    /**
     * 转换为SimInfo
     */
    private SimInfo convertToSimInfo(SimInfoRequest request) {
        return SimInfo.builder()
                .iccid(request.getIccid())
                .imsi(request.getImsi())
                .msisdn(request.getMsisdn())
                .sourceMno(request.getMnoType())
                .build();
    }

    /**
     * SIM信息请求
     */
    @Data
    public static class SimInfoRequest {
        private String iccid;
        private String imsi;
        private String msisdn;
        private String mnoType;
    }

    /**
     * 批量SIM信息请求
     */
    @Data
    public static class BatchSimInfoRequest {
        private List<SimInfoRequest> simList;
    }

    /**
     * 同步数据请求
     */
    @Data
    public static class SyncDataRequest {
        private String hexData;
    }
}
