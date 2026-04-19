package com.g2rain.data.redis;

import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>{@code GenericRedisHelper} 是对 {@link RedisTemplate} 的封装工具类，提供常用的 Redis 操作。</p>
 * <p>支持 Redis 五种常用数据结构的操作：</p>
 * <ul>
 *     <li>Value (字符串类型)</li>
 *     <li>Hash (哈希类型)</li>
 *     <li>List (列表类型)</li>
 *     <li>Set (集合类型)</li>
 *     <li>ZSet (有序集合类型)</li>
 * </ul>
 *
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 * RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
 * GenericRedisHelper redisHelper = new GenericRedisHelper(redisTemplate);
 *
 * // Value 操作
 * redisHelper.set("key1", "value1");
 * String value = redisHelper.get("key1", String.class);
 * redisHelper.set("key2", 100, Duration.ofMinutes(5));
 * redisHelper.increment("key3", 1);
 * redisHelper.decrement("key4", 1);
 * redisHelper.delete("key1");
 *
 * // Hash 操作
 * redisHelper.hSet("hashKey", "field1", "hValue1");
 * redisHelper.hGet("hashKey", "field1", String.class);
 * redisHelper.hMSet("hashKey", Map.of("f2", 20, "f3", 30));
 * redisHelper.hMGet("hashKey", List.of("f2", "f3"), Integer.class);
 * redisHelper.hGetAll("hashKey");
 * redisHelper.hIncrementBy("hashKey", "field2", 5);
 * redisHelper.hDel("hashKey", "field1");
 *
 * // List 操作
 * redisHelper.lPush("listKey", "listValue1");
 * redisHelper.lPop("listKey", String.class);
 * redisHelper.rPush("listKey", "listValue2");
 * redisHelper.rPop("listKey", String.class);
 * redisHelper.lRange("listKey", 0, -1, String.class);
 * redisHelper.lLen("listKey");
 *
 * // Set 操作
 * redisHelper.sAdd("setKey", "setValue1");
 * redisHelper.sAdd("setKey", List.of("setValue2", "setValue3"));
 * redisHelper.sIsMember("setKey", "setValue1");
 * redisHelper.sMembers("setKey", String.class);
 *
 * // ZSet 操作
 * redisHelper.zAdd("zSetKey", "zValue1", 1.0);
 * redisHelper.zRemove("zSetKey", "zValue1");
 * redisHelper.zRange("zSetKey", 0, -1, String.class);
 * redisHelper.zRangeByScore("zSetKey", 0, 10, String.class);
 * redisHelper.zCard("zSetKey");
 *
 * // Key 操作
 * redisHelper.exists("key1");
 * redisHelper.expire("key1", Duration.ofMinutes(10));
 * redisHelper.getExpire("key1");
 * redisHelper.delete(List.of("key1", "key2"));
 * }</pre>
 *
 * <p>微服务中可直接作为 Redis 操作工具使用。</p>
 *
 * @author alpha
 * @since 2025/10/6
 */
public record GenericRedisHelper(RedisTemplate<String, Object> genericRedisTemplate) {

    // ===== Value 操作 =====

    /**
     * 设置键值对
     */
    public <T> void set(String key, T value) {
        genericRedisTemplate.opsForValue().set(key, value);
    }

    /**
     * 带过期时间的 set
     */
    public <T> void set(String key, T value, Duration timeout) {
        genericRedisTemplate.opsForValue().set(key, value, timeout);
    }

    /**
     * 获取指定键的值
     */
    public <T> T get(String key, Class<T> clazz) {
        Object value = genericRedisTemplate.opsForValue().get(key);
        return clazz.cast(value);
    }

    /**
     * 值自增
     */
    public Long increment(String key, long delta) {
        return genericRedisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 值自减
     */
    public Long decrement(String key, long delta) {
        return genericRedisTemplate.opsForValue().increment(key, -delta);
    }

    /**
     * 删除单个 key
     */
    public Boolean delete(String key) {
        return genericRedisTemplate.delete(key);
    }

    // ===== Hash 操作 =====

    /**
     * 设置哈希字段值
     */
    public <T> void hSet(String key, String field, T value) {
        genericRedisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * 获取哈希字段值
     */
    public <T> T hGet(String key, String field, Class<T> clazz) {
        Object value = genericRedisTemplate.opsForHash().get(key, field);
        return clazz.cast(value);
    }

    /**
     * 删除哈希字段
     */
    public Boolean hDel(String key, String field) {
        return genericRedisTemplate.opsForHash().delete(key, field) > 0;
    }

    /**
     * 批量设置哈希字段
     */
    public <T> void hMSet(String key, Map<String, T> map) {
        genericRedisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * 批量获取哈希字段
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> hMGet(String key, Collection<String> fields, Class<T> clazz) {
        return genericRedisTemplate.opsForHash().multiGet(key, (Collection<Object>) (Collection<?>) fields).stream().map(clazz::cast).toList();
    }

    /**
     * 获取哈希所有字段和值
     */
    public Map<Object, Object> hGetAll(String key) {
        return genericRedisTemplate.opsForHash().entries(key);
    }

    /**
     * 哈希字段自增
     */
    public Long hIncrementBy(String key, String field, long delta) {
        return genericRedisTemplate.opsForHash().increment(key, field, delta);
    }

    // ===== List 操作 =====

    /**
     * 左推
     */
    public <T> Long lPush(String key, T value) {
        return genericRedisTemplate.opsForList().leftPush(key, value);
    }

    /**
     * 左弹
     */
    public <T> T lPop(String key, Class<T> clazz) {
        Object value = genericRedisTemplate.opsForList().leftPop(key);
        return clazz.cast(value);
    }

    /**
     * 右推
     */
    public <T> Long rPush(String key, T value) {
        return genericRedisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * 右弹
     */
    public <T> T rPop(String key, Class<T> clazz) {
        Object value = genericRedisTemplate.opsForList().rightPop(key);
        return clazz.cast(value);
    }

    /**
     * 获取列表区间
     */
    public <T> List<T> lRange(String key, long start, long end, Class<T> clazz) {
        List<Object> range = genericRedisTemplate.opsForList().range(key, start, end);
        if (Objects.isNull(range)) return List.of();
        return range.stream().map(clazz::cast).toList();
    }

    /**
     * 列表长度
     */
    public Long lLen(String key) {
        return genericRedisTemplate.opsForList().size(key);
    }

    // ===== Set 操作 =====

    /**
     * 添加单个集合元素
     */
    public <T> Long sAdd(String key, T value) {
        return genericRedisTemplate.opsForSet().add(key, value);
    }

    /**
     * 批量添加集合元素
     */
    public <T> Long sAdd(String key, Collection<T> values) {
        return genericRedisTemplate.opsForSet().add(key, values.toArray());
    }

    /**
     * 判断集合是否包含元素
     */
    public <T> Boolean sIsMember(String key, T value) {
        return genericRedisTemplate.opsForSet().isMember(key, value);
    }

    /**
     * 获取集合所有成员
     */
    public <T> Set<T> sMembers(String key, Class<T> clazz) {
        Set<Object> members = genericRedisTemplate.opsForSet().members(key);
        if (Objects.isNull(members)) return Set.of();
        return members.stream().map(clazz::cast).collect(Collectors.toSet());
    }

    // ===== ZSet 操作 =====

    /**
     * 添加有序集合元素
     */
    public <T> Boolean zAdd(String key, T value, double score) {
        return genericRedisTemplate.opsForZSet().add(key, value, score);
    }

    /**
     * 删除有序集合元素
     */
    public <T> Long zRemove(String key, T value) {
        return genericRedisTemplate.opsForZSet().remove(key, value);
    }

    /**
     * 按索引获取有序集合
     */
    public <T> Set<T> zRange(String key, long start, long end, Class<T> clazz) {
        Set<Object> range = genericRedisTemplate.opsForZSet().range(key, start, end);
        if (Objects.isNull(range)) return Set.of();
        return range.stream().map(clazz::cast).collect(Collectors.toSet());
    }

    /**
     * 按分数获取有序集合
     */
    public <T> Set<T> zRangeByScore(String key, double min, double max, Class<T> clazz) {
        Set<Object> range = genericRedisTemplate.opsForZSet().rangeByScore(key, min, max);
        if (Objects.isNull(range)) return Set.of();
        return range.stream().map(clazz::cast).collect(Collectors.toSet());
    }

    /**
     * 有序集合长度
     */
    public Long zCard(String key) {
        return genericRedisTemplate.opsForZSet().zCard(key);
    }

    // ===== Key 操作 =====

    /**
     * 判断 key 是否存在
     */
    public Boolean exists(String key) {
        return genericRedisTemplate.hasKey(key);
    }

    /**
     * 设置 key 过期时间
     */
    public Boolean expire(String key, Duration timeout) {
        return genericRedisTemplate.expire(key, timeout);
    }

    /**
     * 获取 key 剩余过期时间
     */
    public Long getExpire(String key) {
        return genericRedisTemplate.getExpire(key);
    }

    /**
     * 删除多个 key
     */
    public Long delete(Collection<String> keys) {
        return genericRedisTemplate.delete(keys);
    }
}
