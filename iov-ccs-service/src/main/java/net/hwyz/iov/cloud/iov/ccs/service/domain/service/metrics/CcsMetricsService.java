package net.hwyz.iov.cloud.iov.ccs.service.domain.service.metrics;

/**
 * CCS指标服务接口
 * <p>
 * 采集CMCC/CUCC/MANUAL链路的关键指标
 *
 * @author hwyz_leo
 */
public interface CcsMetricsService {

    // ==================== CMCC指标 ====================

    /**
     * 记录CMCC文件请求耗时
     *
     * @param durationMs 耗时（毫秒）
     * @param success    是否成功
     */
    void recordCmccRequestDuration(long durationMs, boolean success);

    /**
     * 记录CMCC文件下载耗时
     *
     * @param durationMs 耗时（毫秒）
     * @param success    是否成功
     */
    void recordCmccDownloadDuration(long durationMs, boolean success);

    /**
     * 记录CMCC文件解密耗时
     *
     * @param durationMs 耗时（毫秒）
     * @param success    是否成功
     */
    void recordCmccDecryptDuration(long durationMs, boolean success);

    /**
     * 记录CMCC文件解析耗时
     *
     * @param durationMs 耗时（毫秒）
     * @param success    是否成功
     */
    void recordCmccParseDuration(long durationMs, boolean success);

    /**
     * 记录CMCC入库计数
     *
     * @param success   成功数
     * @param duplicate 重复数
     * @param failed    失败数
     */
    void recordCmccStoreCount(int success, int duplicate, int failed);

    // ==================== CUCC指标 ====================

    /**
     * 记录CUCC推送计数
     *
     * @param count 推送数量
     */
    void recordCuccPushCount(int count);

    /**
     * 记录CUCC验签失败
     */
    void recordCuccSignatureFail();

    /**
     * 记录CUCC重放攻击拒绝
     */
    void recordCuccReplayReject();

    /**
     * 记录CUCC入库计数
     *
     * @param success   成功数
     * @param duplicate 重复数
     * @param failed    失败数
     */
    void recordCuccStoreCount(int success, int duplicate, int failed);

    // ==================== MANUAL指标 ====================

    /**
     * 记录MANUAL保存成功
     */
    void recordManualSaveSuccess();

    /**
     * 记录MANUAL保存失败
     *
     * @param reason 失败原因
     */
    void recordManualSaveFail(String reason);

    /**
     * 记录MANUAL批量保存
     *
     * @param totalCount  总数
     * @param failCount   失败数
     */
    void recordManualBatchSave(int totalCount, int failCount);

    /**
     * 记录MANUAL同步数据
     *
     * @param success 是否成功
     * @param count   数据条数
     */
    void recordManualSyncData(boolean success, int count);
}
