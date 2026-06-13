package net.hwyz.iov.cloud.iov.ccs.service.domain.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("分布式锁服务测试")
class LockServiceImplTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private LockServiceImpl lockService;

    @Test
    @DisplayName("获取锁 - 成功返回lockValue")
    void tryLock_success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        String lockValue = lockService.tryLock("ICCID:test", 30);

        assertNotNull(lockValue);
        assertFalse(lockValue.isEmpty());
    }

    @Test
    @DisplayName("获取锁 - 失败返回null")
    void tryLock_failure() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        String lockValue = lockService.tryLock("ICCID:test", 30);

        assertNull(lockValue);
    }

    @Test
    @DisplayName("释放锁 - 成功返回true")
    void unlock_success() {
        when(redisTemplate.execute(any(), anyList(), anyString())).thenReturn(1L);

        boolean result = lockService.unlock("ICCID:test", "lock-value");

        assertTrue(result);
    }

    @Test
    @DisplayName("释放锁 - 锁已过期返回false")
    void unlock_expired() {
        when(redisTemplate.execute(any(), anyList(), anyString())).thenReturn(0L);

        boolean result = lockService.unlock("ICCID:test", "lock-value");

        assertFalse(result);
    }

    @Test
    @DisplayName("带锁执行 - 获取锁成功执行操作")
    void executeWithLock_success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(redisTemplate.execute(any(), anyList(), anyString())).thenReturn(1L);

        String result = lockService.executeWithLock("ICCID:test", 30, () -> "operation_result");

        assertEquals("operation_result", result);
    }

    @Test
    @DisplayName("带锁执行 - 获取锁失败抛异常")
    void executeWithLock_failure() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        assertThrows(IllegalStateException.class,
                () -> lockService.executeWithLock("ICCID:test", 30, () -> "result"));
    }

    @Test
    @DisplayName("带锁执行 - 操作异常时仍释放锁")
    void executeWithLock_exceptionStillUnlocks() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(redisTemplate.execute(any(), anyList(), anyString())).thenReturn(1L);

        assertThrows(RuntimeException.class,
                () -> lockService.executeWithLock("ICCID:test", 30, () -> {
                    throw new RuntimeException("操作失败");
                }));

        // 验证释放锁被调用
        verify(redisTemplate).execute(any(), anyList(), anyString());
    }
}
