package com.g2rain.data.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("GenericRedisHelper测试")
class GenericRedisHelperTest {

    private RedisTemplate<String, Object> mockRedisTemplate;
    private GenericRedisHelper genericRedisHelper;
    private ValueOperations<String, Object> mockValueOps;
    private HashOperations<String, Object, Object> mockHashOps;
    private ListOperations<String, Object> mockListOps;
    private SetOperations<String, Object> mockSetOps;
    private ZSetOperations<String, Object> mockZSetOps;

    @BeforeEach
    void setUp() {
        mockRedisTemplate = mock(RedisTemplate.class);
        genericRedisHelper = new GenericRedisHelper(mockRedisTemplate);

        // Mock各种操作
        mockValueOps = mock(ValueOperations.class);
        mockHashOps = mock(HashOperations.class);
        mockListOps = mock(ListOperations.class);
        mockSetOps = mock(SetOperations.class);
        mockZSetOps = mock(ZSetOperations.class);

        when(mockRedisTemplate.opsForValue()).thenReturn(mockValueOps);
        when(mockRedisTemplate.opsForHash()).thenReturn((HashOperations) mockHashOps);
        when(mockRedisTemplate.opsForList()).thenReturn(mockListOps);
        when(mockRedisTemplate.opsForSet()).thenReturn(mockSetOps);
        when(mockRedisTemplate.opsForZSet()).thenReturn(mockZSetOps);
    }

    @Test
    @DisplayName("测试设置键值对")
    void testSet() {
        String key = "test-key";
        Object value = "test-value";

        genericRedisHelper.set(key, value);

        verify(mockValueOps).set(key, value);
    }

    @Test
    @DisplayName("测试获取键值")
    void testGet() {
        String key = "test-key";
        String expectedValue = "test-value";
        Class<String> clazz = String.class;
        when(mockValueOps.get(key)).thenReturn(expectedValue);

        String result = genericRedisHelper.get(key, clazz);

        verify(mockValueOps).get(key);
        assertEquals(expectedValue, result);
    }

    @Test
    @DisplayName("测试删除键")
    void testDelete() {
        String key = "test-key";
        Boolean expectedReturn = true;
        when(mockRedisTemplate.delete(key)).thenReturn(expectedReturn);

        Boolean result = genericRedisHelper.delete(key);

        verify(mockRedisTemplate).delete(key);
        assertEquals(expectedReturn, result);
    }

    @Test
    @DisplayName("测试Hash设置字段")
    void testHSet() {
        String key = "test-hash";
        String field = "test-field";
        Object value = "test-value";

        genericRedisHelper.hSet(key, field, value);

        verify(mockHashOps).put(key, field, value);
    }

    @Test
    @DisplayName("测试Hash获取字段")
    void testHGet() {
        String key = "test-hash";
        String field = "test-field";
        String expectedValue = "test-value";
        Class<String> clazz = String.class;
        when(mockHashOps.get(key, field)).thenReturn(expectedValue);

        String result = genericRedisHelper.hGet(key, field, clazz);

        verify(mockHashOps).get(key, field);
        assertEquals(expectedValue, result);
    }

    @Test
    @DisplayName("测试Hash删除字段")
    void testHDel() {
        String key = "test-hash";
        String field = "test-field";
        Long deleteCount = 1L;
        when(mockHashOps.delete(key, field)).thenReturn(deleteCount);

        Boolean result = genericRedisHelper.hDel(key, field);

        verify(mockHashOps).delete(key, field);
        assertTrue(result);
    }

    @Test
    @DisplayName("测试List左推入")
    void testLPush() {
        String key = "test-list";
        Object value = "test-value";
        Long expectedReturn = 1L;
        when(mockListOps.leftPush(key, value)).thenReturn(expectedReturn);

        Long result = genericRedisHelper.lPush(key, value);

        verify(mockListOps).leftPush(key, value);
        assertEquals(expectedReturn, result);
    }

    @Test
    @DisplayName("测试List左弹出")
    void testLPop() {
        String key = "test-list";
        String expectedValue = "test-value";
        Class<String> clazz = String.class;
        when(mockListOps.leftPop(key)).thenReturn(expectedValue);

        String result = genericRedisHelper.lPop(key, clazz);

        verify(mockListOps).leftPop(key);
        assertEquals(expectedValue, result);
    }

    @Test
    @DisplayName("测试Set添加元素")
    void testSAdd() {
        String key = "test-set";
        Object value = "test-value";
        Long expectedReturn = 1L;
        when(mockSetOps.add(key, value)).thenReturn(expectedReturn);

        Long result = genericRedisHelper.sAdd(key, value);

        verify(mockSetOps).add(key, value);
        assertEquals(expectedReturn, result);
    }

    @Test
    @DisplayName("测试Set成员检查")
    void testSIsMember() {
        String key = "test-set";
        Object value = "test-value";
        Boolean expectedReturn = true;
        when(mockSetOps.isMember(key, value)).thenReturn(expectedReturn);

        Boolean result = genericRedisHelper.sIsMember(key, value);

        verify(mockSetOps).isMember(key, value);
        assertEquals(expectedReturn, result);
    }

    @Test
    @DisplayName("测试ZSet添加元素")
    void testZAdd() {
        String key = "test-zset";
        Object value = "test-value";
        double score = 1.0;
        Boolean expectedReturn = true;
        when(mockZSetOps.add(key, value, score)).thenReturn(expectedReturn);

        Boolean result = genericRedisHelper.zAdd(key, value, score);

        verify(mockZSetOps).add(key, value, score);
        assertEquals(expectedReturn, result);
    }

    @Test
    @DisplayName("测试ZSet移除元素")
    void testZRemove() {
        String key = "test-zset";
        Object value = "test-value";
        Long expectedReturn = 1L;
        when(mockZSetOps.remove(key, value)).thenReturn(expectedReturn);

        Long result = genericRedisHelper.zRemove(key, value);

        verify(mockZSetOps).remove(key, value);
        assertEquals(expectedReturn, result);
    }
}
