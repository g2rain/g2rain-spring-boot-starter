package com.g2rain.data.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("Redis自动配置测试")
class RedisAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class));

    @Test
    @DisplayName("测试StringRedisHelper Bean创建")
    void testStringRedisHelperBeanCreation() {
        contextRunner
            .withBean("stringRedisTemplate", StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
            .withBean("redisTemplate", RedisTemplate.class, () -> {
                @SuppressWarnings("unchecked")
                RedisTemplate<String, Object> mockTemplate = mock(RedisTemplate.class);
                return mockTemplate;
            })
            .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
            .run(context -> {
                assertThat(context.containsBean("stringRedisHelper")).isTrue();
            });
    }

    @Test
    @DisplayName("测试GenericRedisHelper Bean创建")
    void testGenericRedisHelperBeanCreation() {
        contextRunner
            .withBean("stringRedisTemplate", StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
            .withBean("redisTemplate", RedisTemplate.class, () -> {
                @SuppressWarnings("unchecked")
                RedisTemplate<String, Object> mockTemplate = mock(RedisTemplate.class);
                return mockTemplate;
            })
            .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
            .run(context -> {
                assertThat(context.containsBean("genericRedisHelper")).isTrue();
            });
    }

    @Test
    @DisplayName("测试DistributedLock Bean创建")
    void testDistributedLockBeanCreation() {
        contextRunner
            .withBean("stringRedisTemplate", StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
            .withBean("redisTemplate", RedisTemplate.class, () -> {
                @SuppressWarnings("unchecked")
                RedisTemplate<String, Object> mockTemplate = mock(RedisTemplate.class);
                return mockTemplate;
            })
            .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
            .run(context -> {
                assertThat(context.containsBean("distributedLock")).isTrue();
            });
    }

    @Test
    @DisplayName("测试所有Bean创建")
    void testAllBeansCreation() {
        contextRunner
            .withBean("stringRedisTemplate", StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
            .withBean("redisTemplate", RedisTemplate.class, () -> {
                @SuppressWarnings("unchecked")
                RedisTemplate<String, Object> mockTemplate = mock(RedisTemplate.class);
                return mockTemplate;
            })
            .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
            .run(context -> {
                assertThat(context).hasSingleBean(StringRedisHelper.class);
                assertThat(context).hasSingleBean(GenericRedisHelper.class);
                assertThat(context).hasSingleBean(DistributedLock.class);
            });
    }
}
