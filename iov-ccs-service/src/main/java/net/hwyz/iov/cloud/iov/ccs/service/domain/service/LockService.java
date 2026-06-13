package net.hwyz.iov.cloud.iov.ccs.service.domain.service;

/**
 * 分布式锁服务接口
 * <p>
 * 提供基于Redis的分布式锁能力，Key格式: LOCK:SIM:ICCID:{iccid}，TTL可配置
 *
 * @author hwyz_leo
 */
public interface LockService {

    /**
     * 尝试获取锁
     *
     * @param lockKey    锁的Key（不含前缀，如 ICCID:{iccid}）
     * @param timeoutSec 锁超时时间（秒）
     * @return 锁的value，获取失败返回null
     */
    String tryLock(String lockKey, long timeoutSec);

    /**
     * 释放锁
     *
     * @param lockKey  锁的Key（不含前缀）
     * @param lockValue 锁的value（tryLock返回的值）
     * @return 是否释放成功
     */
    boolean unlock(String lockKey, String lockValue);

    /**
     * 执行带锁的操作
     * <p>
     * 自动获取锁、执行操作、释放锁
     *
     * @param lockKey    锁的Key（不含前缀）
     * @param timeoutSec 锁超时时间（秒）
     * @param action     要执行的操作
     * @return 操作结果
     * @throws IllegalStateException 如果获取锁失败
     */
    <T> T executeWithLock(String lockKey, long timeoutSec, java.util.function.Supplier<T> action);
}
