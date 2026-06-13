package net.hwyz.iov.cloud.iov.ccs.service.domain.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.redis.service.RedisService;
import net.hwyz.iov.cloud.iov.ccs.service.domain.service.LockService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁服务实现
 * <p>
 * 基于Redis SETNX + Lua脚本实现分布式锁
 * Key格式: LOCK:SIM:ICCID:{iccid}
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LockServiceImpl implements LockService {

    private static final String LOCK_PREFIX = "LOCK:SIM:";

    private static final String UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) else return 0 end";

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public String tryLock(String lockKey, long timeoutSec) {
        String fullKey = LOCK_PREFIX + lockKey;
        String lockValue = UUID.randomUUID().toString();

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(fullKey, lockValue, timeoutSec, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(success)) {
            log.debug("获取分布式锁成功: key={}", fullKey);
            return lockValue;
        }

        log.debug("获取分布式锁失败: key={}", fullKey);
        return null;
    }

    @Override
    public boolean unlock(String lockKey, String lockValue) {
        String fullKey = LOCK_PREFIX + lockKey;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_LUA, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(fullKey), lockValue);

        boolean unlocked = result != null && result == 1L;
        if (unlocked) {
            log.debug("释放分布式锁成功: key={}", fullKey);
        } else {
            log.debug("释放分布式锁失败（锁已过期或不属于当前持有者）: key={}", fullKey);
        }
        return unlocked;
    }

    @Override
    public <T> T executeWithLock(String lockKey, long timeoutSec, Supplier<T> action) {
        String lockValue = tryLock(lockKey, timeoutSec);
        if (lockValue == null) {
            throw new IllegalStateException("获取分布式锁失败: " + LOCK_PREFIX + lockKey);
        }

        try {
            return action.get();
        } finally {
            unlock(lockKey, lockValue);
        }
    }
}
