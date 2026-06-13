package net.hwyz.iov.cloud.iov.ccs.service.application.cmcc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * CMCC同步定时任务
 * <p>
 * 每天凌晨3:00触发，从中国移动获取新开户的SIM卡信息
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CmccSyncTask {

    private final CmccFileService cmccFileService;

    /**
     * 每天凌晨3:00执行文件请求任务
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void executeFileRequest() {
        log.info("开始执行CMCC文件请求定时任务");

        try {
            cmccFileService.requestFile();
            log.info("CMCC文件请求定时任务执行完成");
        } catch (Exception e) {
            log.error("CMCC文件请求定时任务执行失败", e);
        }
    }

    /**
     * 每5分钟检查并重试失败的任务
     * <p>
     * 对于PARSED/FAILED且retry_count未超限的批次继续推进
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void retryFailedTasks() {
        log.debug("检查并重试失败的CMCC任务");

        // TODO: 实现重试逻辑
        // 1. 查询状态为PARSED/FAILED且retry_count < 3的记录
        // 2. 对于每条记录，调用processFile重新处理
        // 3. 更新retry_count
        // 4. 超过重试次数的记录，设置为FAILED并发送告警
    }
}
