package com.g2rain.data.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("StringRedisHelper测试")
class StringRedisHelperTest {

    private StringRedisTemplate mockRedisTemplate;
    private StringRedisHelper stringRedisHelper;
    private ValueOperations<String, String> mockValueOps;
    private HashOperations<String, Object, Object> mockHashOps;
    private ListOperations<String, String> mockListOps;
    private SetOperations<String, String> mockSetOps;
    private ZSetOperations<String, String> mockZSetOps;

    @BeforeEach
    void setUp() {
        mockRedisTemplate = mock(StringRedisTemplate.class);
        stringRedisHelper = new StringRedisHelper(mockRedisTemplate);

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
        String value = "test-value";

        Boolean result = stringRedisHelper.set(key, value);

        verify(mockValueOps).set(key, value);
        assertTrue(result);
    }

    @Test
    @DisplayName("测试获取键值")
    void testGet() {
        String key = "test-key";
        String expectedValue = "test-value";
        when(mockValueOps.get(key)).thenReturn(expectedValue);

        String result = stringRedisHelper.get(key);

        verify(mockValueOps).get(key);
        assertEquals(expectedValue, result);
    }

    @Test
    @DisplayName("测试删除键")
    void testDelete() {
        String key = "test-key";
        Boolean expectedReturn = true;
        when(mockRedisTemplate.delete(key)).thenReturn(expectedReturn);

        Boolean result = stringRedisHelper.delete(key);

        verify(mockRedisTemplate).delete(key);
        assertEquals(expectedReturn, result);
    }

    @Test
    @DisplayName("测试Hash设置字段")
    void testHSet() {
        String key = "test-hash";
        String field = "test-field";
        String value = "test-value";

        stringRedisHelper.hSet(key, field, value);

        verify(mockHashOps).put(key, field, value);
    }

    @Test
    @DisplayName("测试Hash获取字段")
    void testHGet() {
        String key = "test-hash";
        String field = "test-field";
        String expectedValue = "test-value";
        when(mockHashOps.get(key, field)).thenReturn(expectedValue);

        String result = stringRedisHelper.hGet(key, field);

        verify(mockHashOps).get(key, field);
        assertEquals(expectedValue, result);
    }

    @Test
    @DisplayName("测试获取整个Hash")
    void testHGetAll() {
        String key = "test-hash";
        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put("field1", "value1");
        expectedMap.put("field2", "value2");
        when(mockHashOps.entries(key)).thenReturn((Map) expectedMap);

        Map<String, String> result = stringRedisHelper.hGetAll(key);

        verify(mockHashOps).entries(key);
        assertEquals(expectedMap, result);
    }

    @Test
    @DisplayName("测试Hash删除字段")
    void testHDel() {
        String key = "test-hash";
        String field = "test-field";
        Long deleteCount = 1L;
        when(mockHashOps.delete(key, field)).thenReturn(deleteCount);

        Boolean result = stringRedisHelper.hDel(key, field);

        verify(mockHashOps).delete(key, field);
        assertTrue(result);
    }

    @Test
    @DisplayName("测试List左推入")
    void testLPush() {
        String key = "test-list";
        String value = "test-value";
        Long expectedReturn = 1L;
        when(mockListOps.leftPush(key, value)).thenReturn(expectedReturn);

        Long result = stringRedisHelper.lPush(key, value);

        verify(mockListOps).leftPush(key, value);
        assertEquals(expectedReturn, result);
    }

    @Test
    @DisplayName("测试List左弹出")
    void testLPop() {
        String key = "test-list";
        String expectedValue = "test-value";
        when(mockListOps.leftPop(key)).thenReturn(expectedValue);

        String result = stringRedisHelper.lPop(key);

        verify(mockListOps).leftPop(key);
        assertEquals(expectedValue, result);
    }

    @Test
    @DisplayName("测试List范围获取")
    void testLRange() {
        String key = "test-list";
        long start = 0L;
        long end = -1L;
        List<String> expectedList = Arrays.asList("value1", "value2");
        when(mockListOps.range(key, start, end)).thenReturn(expectedList);

        List<String> result = stringRedisHelper.lRange(key, start, end);

        verify(mockListOps).range(key, start, end);
        assertEquals(expectedList, result);
    }

    @Test
    @DisplayName("测试Set添加元素")
    void testSAdd() {
        String key = "test-set";
        String value = "test-value";
        Long expectedReturn = 1L;
        when(mockSetOps.add(key, value)).thenReturn(expectedReturn);

        Long result = stringRedisHelper.sAdd(key, value);

        verify(mockSetOps).add(key, value);
        assertEquals(expectedReturn, result);
    }

    @Test
    @DisplayName("测试获取Set所有成员")
    void testSMembers() {
        String key = "test-set";
        Set<String> expectedSet = new HashSet<>(Arrays.asList("value1", "value2"));
        when(mockSetOps.members(key)).thenReturn(expectedSet);

        Set<String> result = stringRedisHelper.sMembers(key);

        verify(mockSetOps).members(key);
        assertEquals(expectedSet, result);
    }

    @Test
    @DisplayName("测试Set成员检查")
    void testSIsMember() {
        String key = "test-set";
        String value = "test-value";
        Boolean expectedReturn = true;
        when(mockSetOps.isMember(key, value)).thenReturn(expectedReturn);

        Boolean result = stringRedisHelper.sIsMember(key, value);

        verify(mockSetOps).isMember(key, value);
        assertEquals(expectedReturn, result);
    }

    @Test
    @DisplayName("测试ZSet添加元素")
    void testZAdd() {
        String key = "test-zset";
        String value = "test-value";
        double score = 1.0;
        Boolean expectedReturn = true;
        when(mockZSetOps.add(key, value, score)).thenReturn(expectedReturn);

        Boolean result = stringRedisHelper.zAdd(key, value, score);

        verify(mockZSetOps).add(key, value, score);
        assertEquals(expectedReturn, result);
    }

    @Test
    @DisplayName("测试ZSet范围获取")
    void testZRange() {
        String key = "test-zset";
        long start = 0L;
        long end = -1L;
        Set<String> expectedSet = new LinkedHashSet<>(Arrays.asList("value1", "value2"));
        when(mockZSetOps.range(key, start, end)).thenReturn(expectedSet);

        Set<String> result = stringRedisHelper.zRange(key, start, end);

        verify(mockZSetOps).range(key, start, end);
        assertEquals(expectedSet, result);
    }

    @Test
    @DisplayName("测试ZSet移除元素")
    void testZRemove() {
        String key = "test-zset";
        String value = "test-value";
        Long expectedReturn = 1L;
        when(mockZSetOps.remove(key, value)).thenReturn(expectedReturn);

        Long result = stringRedisHelper.zRemove(key, value);

        verify(mockZSetOps).remove(key, value);
        assertEquals(expectedReturn, result);
    }
}
