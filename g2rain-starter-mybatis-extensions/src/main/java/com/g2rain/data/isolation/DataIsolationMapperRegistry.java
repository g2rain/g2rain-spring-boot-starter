package com.g2rain.data.isolation;

import com.g2rain.data.isolation.annotations.DataIsolation;
import com.g2rain.data.isolation.annotations.IgnoreIsolation;
import com.g2rain.data.isolation.model.DataIsolationMeta;
import org.apache.ibatis.binding.MapperRegistry;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

/**
 * 支持数据隔离注解扫描的 Mapper 注册器。
 * <p>
 * 在 Mapper 注册阶段扫描 {@link DataIsolation} 与 {@link IgnoreIsolation}，
 * 并将方法级数据隔离元信息写入缓存，供运行期拦截器快速读取。
 * </p>
 *
 * @author alpha
 * @since 2026/3/8
 */
public class DataIsolationMapperRegistry extends MapperRegistry {

    /**
     * 构造函数。
     *
     * @param config MyBatis 配置对象
     */
    public DataIsolationMapperRegistry(Configuration config) {
        super(config);
    }

    /**
     * 注册 Mapper 并同步扫描数据隔离注解信息。
     *
     * @param type Mapper 接口类型
     * @param <T>  Mapper 泛型
     */
    @Override
    public <T> void addMapper(Class<T> type) {
        // 执行原始的流程
        super.addMapper(type);

        // 扫描并缓存注解
        scanMapper(type);
    }

    private void scanMapper(Class<?> mapperClass) {
        DataIsolation dataIsolation = mapperClass.getAnnotation(
            DataIsolation.class
        );

        if (Objects.isNull(dataIsolation)) {
            return;
        }

        if (noneIsolationEnabled(dataIsolation)) {
            return;
        }

        // 类全路径名
        String mapperClassName = mapperClass.getName();
        // 数据隔离元数据
        DataIsolationMeta meta = null;
        // 获取所有的 Mapper 方法进行缓存
        for (Method method : mapperClass.getMethods()) {
            if (!isMapperMethod(method)) {
                continue;
            }

            IgnoreIsolation ignore = method.getAnnotation(
                IgnoreIsolation.class
            );

            if (Objects.nonNull(ignore)) {
                continue;
            }

            String sqlId = mapperClassName.concat(".").concat(method.getName());
            if (DataIsolationCache.containsMeta(sqlId)) {
                continue;
            }

            if (Objects.isNull(meta)) {
                meta = DataIsolationMeta.genObjectByAnnotation(dataIsolation);
            }

            DataIsolationCache.putMeta(sqlId, meta);
        }
    }

    private boolean isMapperMethod(Method method) {
        if (method.getDeclaringClass() == Object.class) {
            return false;
        }

        if (method.isDefault()) {
            return false;
        }

        if (method.isBridge()) {
            return false;
        }

        if (method.isSynthetic()) {
            return false;
        }

        return !Modifier.isStatic(method.getModifiers());
    }

    /**
     * 判断注解配置是否等效于“未启用隔离”。
     *
     * @param dataIsolation 数据隔离注解
     * @return {@code true} 表示未启用
     */
    public boolean noneIsolationEnabled(DataIsolation dataIsolation) {
        // 未来若增加维度(如 userId),此处应改为:!di.organId() && !di.userId()
        return !dataIsolation.organIdIsolation();
    }
}
