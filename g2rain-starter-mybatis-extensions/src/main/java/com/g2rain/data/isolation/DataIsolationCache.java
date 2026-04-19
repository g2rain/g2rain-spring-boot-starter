package com.g2rain.data.isolation;

import com.g2rain.data.isolation.model.DataIsolationMeta;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>数据隔离元信息缓存类，用于存储和快速访问 {@link DataIsolationMeta} 对象。</p>
 * <p>通过静态方法管理缓存，支持存、取、判断是否存在。</p>
 * <p>使用示例：</p>
 * <pre>{@code
 * DataIsolationMeta meta = ...;
 * DataIsolationCache.putMeta("key1", meta);
 * DataIsolationMeta cachedMeta = DataIsolationCache.getMeta("key1");
 * boolean exists = DataIsolationCache.containsMeta("key1");
 * }</pre>
 *
 * @author alpha
 * @since 2025/10/13
 */
public class DataIsolationCache {

    /**
     * <p>私有构造方法，防止实例化。</p>
     */
    private DataIsolationCache() {

    }

    /**
     * <p>缓存存储，Key 为字符串，Value 为 {@link DataIsolationMeta}。</p>
     */
    private static final Map<String, DataIsolationMeta> ISOLATION_ANNOTATION_META_CACHE = new ConcurrentHashMap<>();

    /**
     * <p>向缓存中存储 {@link DataIsolationMeta} 对象。</p>
     *
     * @param key  缓存键
     * @param meta 数据隔离元信息对象
     */
    public static void putMeta(String key, DataIsolationMeta meta) {
        ISOLATION_ANNOTATION_META_CACHE.put(key, meta);
    }

    /**
     * <p>从缓存中获取 {@link DataIsolationMeta} 对象。</p>
     *
     * @param key 缓存键
     * @return 对应的 {@link DataIsolationMeta} 对象，如果不存在返回 {@code null}
     */
    public static DataIsolationMeta getMeta(String key) {
        return ISOLATION_ANNOTATION_META_CACHE.get(key);
    }

    /**
     * <p>判断缓存中是否存在指定 key 的 {@link DataIsolationMeta} 对象。</p>
     *
     * @param key 缓存键
     * @return {@code true} 表示存在，{@code false} 表示不存在
     */
    public static boolean containsMeta(String key) {
        return ISOLATION_ANNOTATION_META_CACHE.containsKey(key);
    }
}
