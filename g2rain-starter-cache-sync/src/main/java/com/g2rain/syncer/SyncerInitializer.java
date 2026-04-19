package com.g2rain.syncer;


import com.g2rain.common.syncer.MessageStorageRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * Syncer 初始化器。
 *
 * <p>负责在 Spring 容器中所有单例 Bean 初始化完成后，对 {@link MessageStorageRegistry}
 * 中注册的所有 {@link com.g2rain.common.syncer.AbstractMessageStorage} 实例执行缓存加载操作。</p>
 *
 * <p>特点与注意事项：</p>
 * <ul>
 *     <li>实现 {@link SmartInitializingSingleton} 接口，确保在所有单例 Bean 初始化完成后才执行。</li>
 *     <li>仅负责首次缓存初始化，不干涉业务微服务后续的 reload 或缓存操作。</li>
 *     <li>若未注册任何 storage，则该初始化器为空操作。</li>
 *     <li>若加载过程中出现异常，会记录 WARN 日志但不会抛出，避免影响服务启动。</li>
 * </ul>
 *
 * @author alpha
 * @since 2025/10/21
 */
@Slf4j
public class SyncerInitializer implements SmartInitializingSingleton {

    /**
     * 当 Spring 容器中所有单例 Bean 初始化完成后触发。
     *
     * <p>遍历 {@link MessageStorageRegistry#getMessageStorages()}，
     * 对每个 {@link com.g2rain.common.syncer.AbstractMessageStorage} 执行 {@link com.g2rain.common.syncer.AbstractMessageStorage#load()} 方法，
     * 完成初始缓存加载。</p>
     *
     * <p>异常处理：若单个存储加载失败，仅记录日志，不抛出异常。</p>
     */
    @Override
    public void afterSingletonsInstantiated() {
        MessageStorageRegistry.getMessageStorages().forEach(storage -> {
            try {
                // 只做“首次装载”：失败不阻断启动，由业务侧/运维侧根据日志与健康检查介入处理。
                storage.load();
            } catch (Exception e) {
                log.warn("加载缓存数据失败", e);
            }
        });
    }
}
