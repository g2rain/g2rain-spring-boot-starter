package com.g2rain.data.redis;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.SystemErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DistributedLock测试")
class DistributedLockTest {

    private RedissonClient mockRedissonClient;
    private DistributedLock distributedLock;
    private RLock mockLock;

    @BeforeEach
    void setUp() {
        mockRedissonClient = mock(RedissonClient.class);
        distributedLock = new DistributedLock(mockRedissonClient);
        mockLock = mock(RLock.class);
    }

    @Test
    @DisplayName("测试手动加锁带租约时间")
    void testLockManualWithLeaseTime() {
        String lockKey = "test-lock";
        Duration leaseTime = Duration.ofSeconds(10);
        when(mockRedissonClient.getLock(lockKey)).thenReturn(mockLock);

        RLock result = distributedLock.lockManual(lockKey, leaseTime, false);

        try {
            verify(mockLock).lock(leaseTime.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            fail("验证锁调用失败: " + e.getMessage());
        }
        assertEquals(mockLock, result);
    }

    @Test
    @DisplayName("测试手动加锁看门狗模式")
    void testLockManualWithWatchdog() {
        String lockKey = "test-lock";
        when(mockRedissonClient.getLock(lockKey)).thenReturn(mockLock);

        RLock result = distributedLock.lockManual(lockKey, null, false);

        try {
            verify(mockLock).lock();
        } catch (Exception e) {
            fail("验证锁调用失败: " + e.getMessage());
        }
        assertEquals(mockLock, result);
    }

    @Test
    @DisplayName("测试手动释放锁成功")
    void testUnlockSuccess() {
        String lockName = "test-lock";
        when(mockLock.getName()).thenReturn(lockName);

        assertDoesNotThrow(() -> distributedLock.unlock(mockLock));
        verify(mockLock).unlock();
    }

    @Test
    @DisplayName("测试手动释放锁异常")
    void testUnlockException() {
        String lockName = "test-lock";
        when(mockLock.getName()).thenReturn(lockName);
        doThrow(new RuntimeException("Unlock failed")).when(mockLock).unlock();

        assertDoesNotThrow(() -> distributedLock.unlock(mockLock));
        verify(mockLock).unlock();
    }

    @Test
    @DisplayName("测试看门狗模式加锁执行业务逻辑")
    void testWaitLockWithWatchdog() {
        String lockKey = "test-lock";
        when(mockRedissonClient.getLock(lockKey)).thenReturn(mockLock);

        Supplier<String> businessLogic = () -> "test-result";
        String result = distributedLock.waitLockWithWatchdog(lockKey, false, businessLogic);

        try {
            verify(mockLock).lock();
            verify(mockLock).unlock();
        } catch (Exception e) {
            fail("验证锁调用失败: " + e.getMessage());
        }
        assertEquals("test-result", result);
    }

    @Test
    @DisplayName("测试带租约时间的阻塞加锁")
    void testWaitLockWithLease() {
        String lockKey = "test-lock";
        Duration leaseTime = Duration.ofSeconds(10);
        when(mockRedissonClient.getLock(lockKey)).thenReturn(mockLock);

        Supplier<String> businessLogic = () -> "test-result";
        String result = distributedLock.waitLockWithLease(lockKey, leaseTime, false, businessLogic);

        try {
            verify(mockLock).lock(leaseTime.toMillis(), TimeUnit.MILLISECONDS);
            verify(mockLock).unlock();
        } catch (Exception e) {
            fail("验证锁调用失败: " + e.getMessage());
        }
        assertEquals("test-result", result);
    }

    @Test
    @DisplayName("测试带租约时间的尝试加锁成功")
    void testTryLockWithLeaseSuccess() throws InterruptedException {
        String lockKey = "test-lock";
        Duration waitTime = Duration.ofSeconds(5);
        Duration leaseTime = Duration.ofSeconds(10);
        when(mockRedissonClient.getLock(lockKey)).thenReturn(mockLock);
        when(mockLock.tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS)).thenReturn(true);

        Supplier<String> businessLogic = () -> "test-result";
        String result = distributedLock.tryLockWithLease(lockKey, waitTime, leaseTime, false, businessLogic);

        verify(mockLock).tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS);
        verify(mockLock).unlock();
        assertEquals("test-result", result);
    }

    @Test
    @DisplayName("测试带租约时间的尝试加锁失败")
    void testTryLockWithLeaseFailure() throws InterruptedException {
        String lockKey = "test-lock";
        Duration waitTime = Duration.ofSeconds(5);
        Duration leaseTime = Duration.ofSeconds(10);
        when(mockRedissonClient.getLock(lockKey)).thenReturn(mockLock);
        when(mockLock.tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS)).thenReturn(false);

        Supplier<String> businessLogic = () -> "test-result";
        String result = distributedLock.tryLockWithLease(lockKey, waitTime, leaseTime, false, businessLogic);

        verify(mockLock).tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS);
        verify(mockLock, never()).unlock();
        assertNull(result);
    }

    @Test
    @DisplayName("测试带租约时间的尝试加锁中断异常")
    void testTryLockWithLeaseInterrupted() throws InterruptedException {
        String lockKey = "test-lock";
        Duration waitTime = Duration.ofSeconds(5);
        Duration leaseTime = Duration.ofSeconds(10);
        when(mockRedissonClient.getLock(lockKey)).thenReturn(mockLock);
        when(mockLock.tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS))
            .thenThrow(new InterruptedException("Interrupted"));

        Supplier<String> businessLogic = () -> "test-result";

        BusinessException exception = assertThrows(BusinessException.class, () ->
            distributedLock.tryLockWithLease(lockKey, waitTime, leaseTime, false, businessLogic));

        assertEquals(String.valueOf(SystemErrorCode.SYSTEM_INTERNAL_ERROR.code()), exception.getErrorCode());
        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    @DisplayName("测试看门狗模式加锁执行业务逻辑异常")
    void testWaitLockWithWatchdogException() {
        String lockKey = "test-lock";
        when(mockRedissonClient.getLock(lockKey)).thenReturn(mockLock);
        doThrow(new RuntimeException("Lock failed")).when(mockLock).lock();

        Supplier<String> businessLogic = () -> "test-result";

        BusinessException exception = assertThrows(BusinessException.class, () ->
            distributedLock.waitLockWithWatchdog(lockKey, false, businessLogic));

        assertEquals(String.valueOf(SystemErrorCode.SYSTEM_INTERNAL_ERROR.code()), exception.getErrorCode());
    }

    @Test
    @DisplayName("测试带租约时间的阻塞加锁异常")
    void testWaitLockWithLeaseException() {
        String lockKey = "test-lock";
        Duration leaseTime = Duration.ofSeconds(10);
        when(mockRedissonClient.getLock(lockKey)).thenReturn(mockLock);
        doThrow(new RuntimeException("Lock failed")).when(mockLock).lock(leaseTime.toMillis(), TimeUnit.MILLISECONDS);

        Supplier<String> businessLogic = () -> "test-result";

        BusinessException exception = assertThrows(BusinessException.class, () ->
            distributedLock.waitLockWithLease(lockKey, leaseTime, false, businessLogic));

        assertEquals(String.valueOf(SystemErrorCode.SYSTEM_INTERNAL_ERROR.code()), exception.getErrorCode());
    }
}
