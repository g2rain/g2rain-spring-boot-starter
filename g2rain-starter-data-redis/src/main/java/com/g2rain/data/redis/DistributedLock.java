package com.g2rain.data.redis;


import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.SystemErrorCode;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * <p>{@code DistributedLock} 是基于 Redisson 的分布式锁工具类。</p>
 * <p>
 * 提供多种加锁、释放锁的方法，支持手动加锁、看门狗模式、带 leaseTime 的阻塞加锁以及尝试加锁。
 * 支持公平锁和非公平锁，方便在分布式环境中对共享资源进行并发控制。
 * </p>
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 * RedissonClient redisson = Redisson.create(config);
 * DistributedLock lockManager = new DistributedLock(redisson);
 *
 * // 手动加锁
 * RLock lock = lockManager.lockManual("myLock", Duration.ofSeconds(30), false);
 * try {
 *     // 执行业务逻辑
 * } finally {
 *     lockManager.unlock(lock);
 * }
 *
 * // 看门狗模式加锁
 * String result = lockManager.waitLockWithWatchdog("myLock", false, () -> "success");
 *
 * // 带 leaseTime 阻塞加锁
 * String result2 = lockManager.waitLockWithLease("myLock", Duration.ofSeconds(10), false, () -> "done");
 *
 * // 尝试加锁
 * String result3 = lockManager.tryLockWithLease("myLock", Duration.ofSeconds(5), Duration.ofSeconds(10), false, () -> "tryLockResult");
 * }</pre>
 *
 * @author alpha
 * @since 2025/10/6
 */
@Slf4j
public record DistributedLock(RedissonClient redisson) {

    /**
     * 获取锁实例。
     *
     * @param lockKey 锁的名称
     * @param isFair  是否公平锁
     * @return {@link RLock} 锁实例
     */
    private RLock getLock(String lockKey, boolean isFair) {
        return isFair ? redisson.getFairLock(lockKey) : redisson.getLock(lockKey);
    }

    /**
     * 手动加锁，不自动释放锁。
     *
     * @param lockKey   锁名称
     * @param leaseTime 锁定时间，{@code null} 则启用看门狗模式
     * @param isFair    是否公平锁
     * @return {@link RLock} 加锁后的锁实例
     * @throws RuntimeException 加锁失败时抛出异常
     */
    public RLock lockManual(String lockKey, @Nullable Duration leaseTime, boolean isFair) {
        RLock lock = getLock(lockKey, isFair);
        try {
            if (Objects.nonNull(leaseTime)) {
                lock.lock(leaseTime.toMillis(), TimeUnit.MILLISECONDS);
            } else {
                lock.lock(); // 看门狗模式
            }
        } catch (Exception e) {
            log.error("lockManual error: {}", lockKey, e);
            throw e;
        }
        return lock;
    }

    /**
     * 手动释放锁。
     *
     * @param lock 要释放的锁实例
     */
    public void unlock(RLock lock) {
        try {
            lock.unlock();
            log.debug("Manual unlock success: {}", lock.getName());
        } catch (Exception e) {
            log.warn("Manual unlock failed: {}", lock.getName(), e);
        }
    }

    /**
     * 看门狗模式加锁并执行业务逻辑。
     *
     * @param lockKey       锁名称
     * @param isFair        是否公平锁
     * @param businessLogic 业务逻辑
     * @param <T>           返回值类型
     * @return 业务逻辑返回值
     * @throws BusinessException 加锁失败时抛出
     */
    public <T> T waitLockWithWatchdog(String lockKey, boolean isFair, Supplier<T> businessLogic) {
        return waitLockWithWatchdog(lockKey, isFair, businessLogic, null);
    }

    /**
     * 看门狗模式加锁并执行业务逻辑，支持最终回调。
     *
     * @param lockKey         锁名称
     * @param isFair          是否公平锁
     * @param businessLogic   业务逻辑
     * @param finallyCallback 最终回调
     * @param <T>             返回值类型
     * @return 业务逻辑返回值
     * @throws BusinessException 加锁失败时抛出
     */
    public <T> T waitLockWithWatchdog(String lockKey, boolean isFair, Supplier<T> businessLogic, @Nullable Runnable finallyCallback) {
        RLock lock = getLock(lockKey, isFair);
        try {
            lock.lock(); // 看门狗自动续租
            return executeWithUnlock(lock, businessLogic, finallyCallback);
        } catch (Exception e) {
            throw new BusinessException(SystemErrorCode.SYSTEM_INTERNAL_ERROR, e);
        }
    }

    /**
     * 带 leaseTime 的阻塞加锁。
     *
     * @param lockKey       锁名称
     * @param leaseTime     锁定时间
     * @param isFair        是否公平锁
     * @param businessLogic 业务逻辑
     * @param <T>           返回值类型
     * @return 业务逻辑返回值
     * @throws BusinessException 加锁失败时抛出
     */
    public <T> T waitLockWithLease(String lockKey, Duration leaseTime, boolean isFair, Supplier<T> businessLogic) {
        return waitLockWithLease(lockKey, leaseTime, isFair, businessLogic, null);
    }

    /**
     * 带 leaseTime 的阻塞加锁，支持最终回调。
     *
     * @param lockKey         锁名称
     * @param leaseTime       锁定时间
     * @param isFair          是否公平锁
     * @param businessLogic   业务逻辑
     * @param finallyCallback 最终回调
     * @param <T>             返回值类型
     * @return 业务逻辑返回值
     * @throws BusinessException 加锁失败时抛出
     */

    public <T> T waitLockWithLease(String lockKey, Duration leaseTime, boolean isFair, Supplier<T> businessLogic, @Nullable Runnable finallyCallback) {
        RLock lock = getLock(lockKey, isFair);
        try {
            lock.lock(leaseTime.toMillis(), TimeUnit.MILLISECONDS);
            return executeWithUnlock(lock, businessLogic, finallyCallback);
        } catch (Exception e) {
            throw new BusinessException(SystemErrorCode.SYSTEM_INTERNAL_ERROR, e);
        }
    }

    /**
     * 尝试加锁，成功则执行业务逻辑，失败返回 {@code null}。
     *
     * @param lockKey       锁名称
     * @param waitTime      等待时间
     * @param leaseTime     锁定时间
     * @param isFair        是否公平锁
     * @param businessLogic 业务逻辑
     * @param <T>           返回值类型
     * @return 业务逻辑返回值或 {@code null}
     * @throws BusinessException 加锁失败时抛出
     */
    public <T> T tryLockWithLease(String lockKey, Duration waitTime, Duration leaseTime, boolean isFair, Supplier<T> businessLogic) {
        return tryLockWithLease(lockKey, waitTime, leaseTime, isFair, businessLogic, null);
    }

    /**
     * 尝试加锁，支持最终回调。
     *
     * @param lockKey         锁名称
     * @param waitTime        等待时间
     * @param leaseTime       锁定时间
     * @param isFair          是否公平锁
     * @param businessLogic   业务逻辑
     * @param finallyCallback 最终回调
     * @param <T>             返回值类型
     * @return 业务逻辑返回值或 {@code null}
     * @throws BusinessException 加锁失败时抛出
     */
    public <T> T tryLockWithLease(String lockKey, Duration waitTime, Duration leaseTime, boolean isFair, Supplier<T> businessLogic, @Nullable Runnable finallyCallback) {
        RLock lock = getLock(lockKey, isFair);
        try {
            boolean acquired = lock.tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                log.debug("tryLockWithLease failed to acquire lock: {}", lockKey);
                return null;
            }
            return executeWithUnlock(lock, businessLogic, finallyCallback);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 重新设置中断状态
            throw new BusinessException(SystemErrorCode.SYSTEM_INTERNAL_ERROR, e);
        }
    }

    /**
     * 执行业务逻辑并在最后释放锁。
     *
     * @param lock            锁实例
     * @param businessLogic   业务逻辑
     * @param finallyCallback 最终回调
     * @param <T>             返回值类型
     * @return 业务逻辑返回值
     */
    private <T> T executeWithUnlock(RLock lock, Supplier<T> businessLogic, @Nullable Runnable finallyCallback) {
        try {
            return businessLogic.get();
        } finally {
            safeRunCallback(finallyCallback);
            unlock(lock);
        }
    }

    /**
     * 安全执行回调方法，忽略异常。
     *
     * @param callback 回调方法
     */
    private void safeRunCallback(@Nullable Runnable callback) {
        if (Objects.isNull(callback)) {
            return;
        }
        try {
            callback.run();
        } catch (Exception e) {
            log.warn("finallyCallback execute failed", e);
        }
    }
}
