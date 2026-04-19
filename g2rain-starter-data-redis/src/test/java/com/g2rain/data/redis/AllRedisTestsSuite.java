package com.g2rain.data.redis;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Redis Starter测试套件")
@SelectClasses({
    // 自动配置测试
    RedisAutoConfigurationTest.class,

    // StringRedisHelper测试
    StringRedisHelperTest.class,

    // GenericRedisHelper测试
    GenericRedisHelperTest.class,

    // DistributedLock测试
    DistributedLockTest.class
})
public class AllRedisTestsSuite {
}
