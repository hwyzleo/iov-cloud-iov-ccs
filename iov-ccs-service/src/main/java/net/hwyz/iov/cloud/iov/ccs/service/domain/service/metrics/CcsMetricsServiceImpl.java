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
        log.debug("CMCC请求耗时: {}ms, success={}", durationMs, success);
    }

    @Override
    public void recordCmccDownloadDuration(long durationMs, boolean success) {
        Timer.builder("ccs.cmcc.download.duration")
                .description("CMCC文件下载耗时")
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(durationMs));
        log.debug("CMCC下载耗时: {}ms, success={}", durationMs, success);
    }

    @Override
    public void recordCmccDecryptDuration(long durationMs, boolean success) {
        Timer.builder("ccs.cmcc.decrypt.duration")
                .description("CMCC文件解密耗时")
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(durationMs));
        log.debug("CMCC解密耗时: {}ms, success={}", durationMs, success);
    }

    @Override
    public void recordCmccParseDuration(long durationMs, boolean success) {
        Timer.builder("ccs.cmcc.parse.duration")
                .description("CMCC文件解析耗时")
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(durationMs));
        log.debug("CMCC解析耗时: {}ms, success={}", durationMs, success);
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

        log.debug("CMCC入库统计: success={}, duplicate={}, failed={}", success, duplicate, failed);
    }

    // ==================== CUCC指标 ====================

    @Override
    public void recordCuccPushCount(int count) {
        Counter.builder("ccs.cucc.push.count")
                .description("CUCC推送次数")
                .register(meterRegistry)
                .increment(count);
        log.debug("CUCC推送计数: {}", count);
    }

    @Override
    public void recordCuccSignatureFail() {
        Counter.builder("ccs.cucc.signature.fail")
                .description("CUCC验签失败次数")
                .register(meterRegistry)
                .increment();
        log.debug("CUCC验签失败");
    }

    @Override
    public void recordCuccReplayReject() {
        Counter.builder("ccs.cucc.replay.reject")
                .description("CUCC重放攻击拒绝次数")
                .register(meterRegistry)
                .increment();
        log.debug("CUCC重放攻击拒绝");
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

        log.debug("CUCC入库统计: success={}, duplicate={}, failed={}", success, duplicate, failed);
    }

    // ==================== 手动/同步指标 ====================

    @Override
    public void recordManualSaveSuccess() {
        Counter.builder("ccs.manual.save.success")
                .description("手动保存成功次数")
                .register(meterRegistry)
                .increment();
        log.debug("手动保存成功");
    }

    @Override
    public void recordManualSaveFail(String reason) {
        Counter.builder("ccs.manual.save.fail")
                .description("手动保存失败次数")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
        log.debug("手动保存失败: reason={}", reason);
    }

    @Override
    public void recordManualBatchSave(int totalCount, int failCount) {
        Counter.builder("ccs.manual.batch.save.total")
                .description("手动批量保存总数")
                .register(meterRegistry)
                .increment(totalCount);

        Counter.builder("ccs.manual.batch.save.fail")
                .description("手动批量保存失败数")
                .register(meterRegistry)
                .increment(failCount);

        log.debug("手动批量保存: total={}, fail={}", totalCount, failCount);
    }

    @Override
    public void recordManualSyncData(boolean success, int count) {
        Counter.builder("ccs.manual.sync.data")
                .description("手动同步数据次数")
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .increment();

        if (success) {
            Counter.builder("ccs.manual.sync.data.count")
                    .description("手动同步数据条数")
                    .register(meterRegistry)
                    .increment(count);
        }

        log.debug("手动同步数据: success={}, count={}", success, count);
    }

    // ==================== 车卡绑定指标 ====================

    @Override
    public void recordVmdBindingSuccess() {
        Counter.builder("ccs.vmd.binding.success")
                .description("VMD绑定成功次数")
                .register(meterRegistry)
                .increment();
        log.debug("VMD绑定成功");
    }

    @Override
    public void recordVmdBindingIccidNotFound() {
        Counter.builder("ccs.vmd.binding.iccid.not.found")
                .description("VMD绑定ICCID未命中次数")
                .register(meterRegistry)
                .increment();
        log.debug("VMD绑定ICCID未命中");
    }

    @Override
    public void recordVmdBindingMnoFail() {
        Counter.builder("ccs.vmd.binding.mno.fail")
                .description("VMD绑定运营商调用失败次数")
                .register(meterRegistry)
                .increment();
        log.debug("VMD绑定运营商调用失败");
    }

    @Override
    public void recordVmdBindingLockFail() {
        Counter.builder("ccs.vmd.binding.lock.fail")
                .description("VMD绑定获取锁失败次数")
                .register(meterRegistry)
                .increment();
        log.debug("VMD绑定获取锁失败");
    }

    @Override
    public void recordVmdReconciliationCatchUp(int count) {
        Counter.builder("ccs.vmd.reconciliation.catch.up")
                .description("VMD对账补齐数量")
                .register(meterRegistry)
                .increment(count);
        log.debug("VMD对账补齐: {}", count);
    }

    @Override
    public void recordVmdReconciliationAlert(int count) {
        Counter.builder("ccs.vmd.reconciliation.alert")
                .description("VMD对账告警数量")
                .register(meterRegistry)
                .increment(count);
        log.debug("VMD对账告警: {}", count);
    }

    @Override
    public void recordVmdReconciliationDiff(int count) {
        Counter.builder("ccs.vmd.reconciliation.diff")
                .description("VMD对账差异数量")
                .register(meterRegistry)
                .increment(count);
        log.debug("VMD对账差异: {}", count);
    }

    @Override
    public void recordVmdReconciliationRebind(int count) {
        Counter.builder("ccs.vmd.reconciliation.rebind")
                .description("VMD对账补绑数量")
                .register(meterRegistry)
                .increment(count);
        log.debug("VMD对账补绑: {}", count);
    }

    // ==================== 事件发布指标 ====================

    @Override
    public void recordEventOutboxSaved() {
        Counter.builder("ccs.event.outbox.saved")
                .description("事件写入outbox成功次数")
                .register(meterRegistry)
                .increment();
        log.debug("事件写入outbox成功");
    }

    @Override
    public void recordEventSerializeFail() {
        Counter.builder("ccs.event.serialize.fail")
                .description("事件序列化失败次数")
                .register(meterRegistry)
                .increment();
        log.debug("事件序列化失败");
    }

    @Override
    public void recordEventPublished() {
        Counter.builder("ccs.event.published")
                .description("事件发布成功次数")
                .register(meterRegistry)
                .increment();
        log.debug("事件发布成功");
    }

    @Override
    public void recordEventPublishFail() {
        Counter.builder("ccs.event.publish.fail")
                .description("事件发布失败次数")
                .register(meterRegistry)
                .increment();
        log.debug("事件发布失败");
    }

    @Override
    public void recordEventPublishFailed() {
        Counter.builder("ccs.event.publish.failed")
                .description("事件发布失败超过最大重试次数")
                .register(meterRegistry)
                .increment();
        log.debug("事件发布失败超过最大重试次数");
    }

    @Override
    public void recordEventRetrySuccess() {
        Counter.builder("ccs.event.retry.success")
                .description("事件重试发布成功次数")
                .register(meterRegistry)
                .increment();
        log.debug("事件重试发布成功");
    }
}
