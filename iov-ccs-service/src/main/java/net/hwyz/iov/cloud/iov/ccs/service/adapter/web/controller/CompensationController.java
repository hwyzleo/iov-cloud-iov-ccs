package net.hwyz.iov.cloud.iov.ccs.service.adapter.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ccs.service.application.cmcc.CompensationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 运维补偿接口
 * <p>
 * 提供重跑/扫描能力
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequestMapping("/api/service/compensation")
@RequiredArgsConstructor
public class CompensationController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CompensationService compensationService;

    /**
     * 按fileId重跑CMCC文件处理
     *
     * @param fileId 文件ID
     * @return 响应
     */
    @PostMapping("/cmcc/rerun/file/{fileId}")
    public ResponseEntity<Map<String, Object>> rerunByFileId(@PathVariable String fileId) {
        log.info("运维操作：按fileId重跑: fileId={}", fileId);

        boolean result = compensationService.rerunByFileId(fileId);

        if (result) {
            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "success", true,
                    "msg", "重跑已触发"
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", 400,
                    "success", false,
                    "msg", "重跑失败：记录不存在或已处于终态"
            ));
        }
    }

    /**
     * 按日期范围重跑CMCC文件请求
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 响应
     */
    @PostMapping("/cmcc/rerun/date-range")
    public ResponseEntity<Map<String, Object>> rerunByDateRange(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {

        log.info("运维操作：按日期范围重跑: startDate={}, endDate={}", startDate, endDate);

        try {
            LocalDateTime start = LocalDateTime.parse(startDate, DATE_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endDate, DATE_FORMATTER);

            int count = compensationService.rerunByDateRange(start, end);

            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "success", true,
                    "msg", "重跑完成",
                    "count", count
            ));
        } catch (Exception e) {
            log.error("日期解析失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "code", 400,
                    "success", false,
                    "msg", "日期格式错误: " + e.getMessage()
            ));
        }
    }

    /**
     * 扫描并重试候选表中PARSED/FAILED的记录
     *
     * @return 响应
     */
    @PostMapping("/candidates/scan-retry")
    public ResponseEntity<Map<String, Object>> scanAndRetryCandidates() {
        log.info("运维操作：扫描并重试候选表");

        int count = compensationService.scanAndRetryCandidates();

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "success", true,
                "msg", "扫描完成",
                "count", count
        ));
    }

    /**
     * 获取待重试的CMCC记录列表
     *
     * @return 响应
     */
    @GetMapping("/cmcc/pending-retry")
    public ResponseEntity<Map<String, Object>> listPendingRetryFileIds() {
        log.info("运维操作：获取待重试记录列表");

        List<String> fileIds = compensationService.listPendingRetryFileIds();

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "success", true,
                "data", fileIds
        ));
    }

    /**
     * 获取候选表统计信息
     *
     * @return 响应
     */
    @GetMapping("/candidates/statistics")
    public ResponseEntity<Map<String, Object>> getCandidateStatistics() {
        log.info("运维操作：获取候选表统计信息");

        List<Object[]> statistics = compensationService.getCandidateStatistics();

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "success", true,
                "data", statistics
        ));
    }
}
