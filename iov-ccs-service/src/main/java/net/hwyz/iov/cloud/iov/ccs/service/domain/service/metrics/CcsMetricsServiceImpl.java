package net.hwyz.iov.cloud.iov.ccs.service.domain.service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * CCS指标服务实现
 * <p>
 * 使用Micrometer采集指标，支持Prometheus
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CcsMetricsServiceImpl implements CcsMetricsService {

    private final MeterRegistry meterRegistry;

    // ==================== CMCC指标 ====================

    @Override
    public void recordCmccRequestDuration(long durationMs, boolean success) {
        Timer.builder("ccs.cmcc.request.duration")
                .description("CMCC文件请求耗时")
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(durationMs));
        logger.debug("CMCC请求耗时: {}ms, success={}", durationMs, success);
    }

    @Override
    public void recordCmccDownloadDuration(long durationMs, boolean success) {
        Timer.builder("ccs.cmcc.download.duration")
                .description("CMCC文件下载耗时")
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(durationMs));
        logger.debug("CMCC下载耗时: {}ms, success={}", durationMs, success);
    }

    @Override
    public void recordCmccDecryptDuration(long durationMs, boolean success) {
        Timer.builder("ccs.cmcc.decrypt.duration")
                .description("CMCC文件解密耗时")
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(durationMs));
        logger.debug("CMCC解密耗时: {}ms, success={}", durationMs, success);
    }

    @Override
    public void recordCmccParseDuration(long durationMs, boolean success) {
        Timer.builder("ccs.cmcc.parse.duration")
                .description("CMCC文件解析耗时")
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(durationMs));
        logger.debug("CMCC解析耗时: {}ms, success={}", durationMs, success);
    }

    @Override
    public void recordCmccStoreCount(int success, int duplicate, int failed) {
        Counter.builder("ccs.cmcc.store.success")
                .description("CMCC入库成功数")
                .register(meterRegistry)
                .increment(success);

        Counter.builder("ccs.cmcc.store.duplicate")
                .description("CMCC入库重复数")
                .register(meterRegistry)
                .increment(duplicate);

        Counter.builder("ccs.cmcc.store.failed")
                .description("CMCC入库失败数")
                .register(meterRegistry)
                .increment(failed);

        logger.debug("CMCC入库统计: success={}, duplicate={}, failed={}", success, duplicate, failed);
    }

    // ==================== CUCC指标 ====================

    @Override
    public void recordCuccPushCount(int count) {
        Counter.builder("ccs.cucc.push.count")
                .description("CUCC推送次数")
                .register(meterRegistry)
                .increment(count);
        logger.debug("CUCC推送计数: {}", count);
    }

    @Override
    public void recordCuccSignatureFail() {
        Counter.builder("ccs.cucc.signature.fail")
                .description("CUCC验签失败次数")
                .register(meterRegistry)
                .increment();
        logger.debug("CUCC验签失败");
    }

    @Override
    public void recordCuccReplayReject() {
        Counter.builder("ccs.cucc.replay.reject")
                .description("CUCC重放攻击拒绝次数")
                .register(meterRegistry)
                .increment();
        logger.debug("CUCC重放攻击拒绝");
    }

    @Override
    public void recordCuccStoreCount(int success, int duplicate, int failed) {
        Counter.builder("ccs.cucc.store.success")
                .description("CUCC入库成功数")
                .register(meterRegistry)
                .increment(success);

        Counter.builder("ccs.cucc.store.duplicate")
                .description("CUCC入库重复数")
                .register(meterRegistry)
                .increment(duplicate);

        Counter.builder("ccs.cucc.store.failed")
                .description("CUCC入库失败数")
                .register(meterRegistry)
                .increment(failed);

        logger.debug("CUCC入库统计: success={}, duplicate={}, failed={}", success, duplicate, failed);
    }

    // ==================== MANUAL指标 ====================

    @Override
    public void recordManualSaveSuccess() {
        Counter.builder("ccs.manual.save.success")
                .description("MANUAL保存成功次数")
                .register(meterRegistry)
                .increment();
        logger.debug("MANUAL保存成功");
    }

    @Override
    public void recordManualSaveFail(String reason) {
        Counter.builder("ccs.manual.save.fail")
                .description("MANUAL保存失败次数")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
        logger.debug("MANUAL保存失败: reason={}", reason);
    }

    @Override
    public void recordManualBatchSave(int totalCount, int failCount) {
        Counter.builder("ccs.manual.batch.save.total")
                .description("MANUAL批量保存总数")
                .register(meterRegistry)
                .increment(totalCount);

        Counter.builder("ccs.manual.batch.save.fail")
                .description("MANUAL批量保存失败数")
                .register(meterRegistry)
                .increment(failCount);

        logger.debug("MANUAL批量保存: total={}, fail={}", totalCount, failCount);
    }

    @Override
    public void recordManualSyncData(boolean success, int count) {
        Counter.builder("ccs.manual.sync.data")
                .description("MANUAL同步数据次数")
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .increment();

        if (success) {
            Counter.builder("ccs.manual.sync.data.count")
                    .description("MANUAL同步数据条数")
                    .register(meterRegistry)
                    .increment(count);
        }

        logger.debug("MANUAL同步数据: success={}, count={}", success, count);
    }
}
