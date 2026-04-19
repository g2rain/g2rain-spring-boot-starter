package com.g2rain.data.isolation;


import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.SystemErrorCode;
import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;

import java.lang.reflect.Field;

/**
 * MyBatis 配置定制器。
 * <p>
 * 用于在启动阶段将默认 MapperRegistry 替换为
 * {@link DataIsolationMapperRegistry}，从而支持数据隔离注解扫描能力。
 * </p>
 *
 * @author alpha
 * @since 2026/3/25
 */
public class MybatisMapperCustomizer implements ConfigurationCustomizer {

    /**
     * 覆盖 MyBatis 默认 mapperRegistry 实现。
     *
     * @param configuration MyBatis 配置对象
     */
    @Override
    public void customize(Configuration configuration) {
        try {
            Field field = Configuration.class.getDeclaredField("mapperRegistry");
            field.setAccessible(true);
            field.set(configuration, new DataIsolationMapperRegistry(configuration));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new BusinessException(SystemErrorCode.SYSTEM_INTERNAL_ERROR,
                "Failed to override MyBatis MapperRegistry core container, please check dependency version compatibility."
            );
        }
    }
}
