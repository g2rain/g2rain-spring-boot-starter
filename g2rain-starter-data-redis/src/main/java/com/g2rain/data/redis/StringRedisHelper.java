package com.g2rain.data.redis;

import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 阻塞式 String 类型 Redis 操作工具类，封装了常用的 Redis 数据结构操作：
 * </p>
 * <ul>
 *     <li>Value（字符串类型）</li>
 *     <li>Hash（哈希类型）</li>
 *     <li>List（列表类型）</li>
 *     <li>Set（集合类型）</li>
 *     <li>ZSet（有序集合类型）</li>
 * </ul>
 *
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 * StringRedisTemplate stringRedisTemplate = new StringRedisTemplate();
 * StringRedisHelper redisHelper = new StringRedisHelper(stringRedisTemplate);
 *
 * // Value 操作
 * redisHelper.set("key1", "value1");
 * String value = redisHelper.get("key1");
 * redisHelper.set("key2", "value2", Duration.ofSeconds(60));
 * redisHelper.increment("counter", 1);
 * redisHelper.decrement("counter", 1);
 * redisHelper.delete("key1");
 *
 * // Hash 操作
 * redisHelper.hSet("hashKey", "field1", "hValue1");
 * String hValue = redisHelper.hGet("hashKey", "field1");
 * Map<String, String> allHash = redisHelper.hGetAll("hashKey");
 * redisHelper.hDel("hashKey", "field1");
 *
 * // List 操作
 * redisHelper.lPush("listKey", "listValue1");
 * String listValue = redisHelper.lPop("listKey");
 * redisHelper.rPush("listKey", "listValue2");
 * String rValue = redisHelper.rPop("listKey");
 * List<String> listValues = redisHelper.lRange("listKey", 0, -1);
 * redisHelper.lLen("listKey");
 *
 * // Set 操作
 * redisHelper.sAdd("setKey", "setValue1");
 * Set<String> members = redisHelper.sMembers("setKey");
 * boolean isMember = redisHelper.sIsMember("setKey", "setValue1");
 *
 * // ZSet 操作
 * redisHelper.zAdd("zSetKey", "zValue1", 1.0);
 * Set<String> zsetRange = redisHelper.zRange("zSetKey", 0, -1);
 * Set<String> zsetByScore = redisHelper.zRangeByScore("zSetKey", 0, 10);
 * redisHelper.zRemove("zSetKey", "zValue1");
 * redisHelper.zCard("zSetKey");
 *
 * // Key 操作
 * boolean exists = redisHelper.exists("key1");
 * redisHelper.expire("key1", Duration.ofMinutes(5));
 * long ttl = redisHelper.getExpire("key1");
 * }</pre>
 *
 * @author 孙兴宝
 * @version 1.1
 */
public record StringRedisHelper(StringRedisTemplate stringRedisTemplate) {

    // ===== Value ops =====

    /**
     * 设置字符串键值对
     */
    public Boolean set(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
        return true;
    }

    /**
     * 获取字符串键值
     */
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 删除指定键
     */
    public Boolean delete(String key) {
        return stringRedisTemplate.delete(key);
    }

    /**
     * 带过期时间设置字符串键值对
     */
    public Boolean set(String key, String value, Duration timeout) {
        stringRedisTemplate.opsForValue().set(key, value, timeout);
        return true;
    }

    /**
     * 字符串值自增
     */
    public Long increment(String key, long delta) {
        return stringRedisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 字符串值自减
     */
    public Long decrement(String key, long delta) {
        return stringRedisTemplate.opsForValue().increment(key, -delta);
    }

    /**
     * 判断 key 是否存在
     */
    public Boolean exists(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    /**
     * 设置 key 过期时间
     */
    public Boolean expire(String key, Duration timeout) {
        return stringRedisTemplate.expire(key, timeout);
    }

    /**
     * 获取 key 剩余过期时间
     */
    public Long getExpire(String key) {
        return stringRedisTemplate.getExpire(key);
    }

    // ===== Hash ops =====

    /**
     * 设置哈希字段值
     */
    public void hSet(String key, String field, String value) {
        stringRedisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * 获取哈希字段值
     */
    public String hGet(String key, String field) {
        return (String) stringRedisTemplate.opsForHash().get(key, field);
    }

    /**
     * 获取哈希键的所有字段和值
     */
    public Map<String, String> hGetAll(String key) {
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) {
            return Collections.emptyMap();
        }
        return entries.entrySet().stream().collect(Collectors.toMap(
            e -> (String) e.getKey(),
            e -> (String) e.getValue()
        ));
    }

    /**
     * 删除哈希字段
     */
    public Boolean hDel(String key, String field) {
        return stringRedisTemplate.opsForHash().delete(key, field) > 0;
    }

    /**
     * 哈希字段自增
     */
    public Long hIncrementBy(String key, String field, long delta) {
        return stringRedisTemplate.opsForHash().increment(key, field, delta);
    }

    // ===== List ops =====

    /**
     * 将值推入列表左侧
     */
    public Long lPush(String key, String value) {
        return stringRedisTemplate.opsForList().leftPush(key, value);
    }

    /**
     * 从列表左侧弹出值
     */
    public String lPop(String key) {
        return stringRedisTemplate.opsForList().leftPop(key);
    }

    /**
     * 将值推入列表右侧
     */
    public Long rPush(String key, String value) {
        return stringRedisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * 从列表右侧弹出值
     */
    public String rPop(String key) {
        return stringRedisTemplate.opsForList().rightPop(key);
    }

    /**
     * 获取列表指定区间的值
     */
    public List<String> lRange(String key, long start, long end) {
        return stringRedisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 获取列表长度
     */
    public Long lLen(String key) {
        return stringRedisTemplate.opsForList().size(key);
    }

    // ===== Set ops =====

    /**
     * 向集合添加值
     */
    public Long sAdd(String key, String value) {
        return stringRedisTemplate.opsForSet().add(key, value);
    }

    /**
     * 获取集合所有成员
     */
    public Set<String> sMembers(String key) {
        return stringRedisTemplate.opsForSet().members(key);
    }

    /**
     * 判断集合是否包含某个值
     */
    public Boolean sIsMember(String key, String value) {
        return stringRedisTemplate.opsForSet().isMember(key, value);
    }

    /**
     * 批量添加集合元素
     */
    public Long sAdd(String key, Collection<String> values) {
        return stringRedisTemplate.opsForSet().add(key, values.toArray(new String[0]));
    }

    // ===== ZSet ops =====

    /**
     * 向有序集合添加值及分数
     */
    public Boolean zAdd(String key, String value, double score) {
        return stringRedisTemplate.opsForZSet().add(key, value, score);
    }

    /**
     * 获取有序集合指定范围的值
     */
    public Set<String> zRange(String key, long start, long end) {
        return stringRedisTemplate.opsForZSet().range(key, start, end);
    }

    /**
     * 按分数区间获取有序集合值
     */
    public Set<String> zRangeByScore(String key, double min, double max) {
        return stringRedisTemplate.opsForZSet().rangeByScore(key, min, max);
    }

    /**
     * 从有序集合移除值
     */
    public Long zRemove(String key, String value) {
        return stringRedisTemplate.opsForZSet().remove(key, value);
    }

    /**
     * 获取有序集合长度
     */
    public Long zCard(String key) {
        return stringRedisTemplate.opsForZSet().zCard(key);
    }

    public <T extends @NonNull Object> T execute(@NonNull RedisScript<T> script, @NonNull List<@NonNull String> keys, @NonNull Object @NonNull ... args) {
        return stringRedisTemplate.execute(script, keys, args);
    }
}
