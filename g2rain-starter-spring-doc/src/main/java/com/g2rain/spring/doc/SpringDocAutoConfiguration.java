package com.g2rain.spring.doc;

import com.g2rain.common.model.Result;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.PropertyCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Objects;

/**
 * SpringDoc / OpenAPI 公共配置：引入本 starter 即可生效，各微服务可通过配置覆盖文档描述等。
 *
 * @author alpha
 * @since 2026/4/9
 */
@AutoConfiguration
@ConditionalOnClass(name = {
    "org.springdoc.core.customizers.PropertyCustomizer",
    "io.swagger.v3.oas.models.OpenAPI"
})
public class SpringDocAutoConfiguration {

    /**
     * 文档 Info：标题默认使用 {@code spring.application.name}；
     * {@code g2rain.springdoc.description} 为空时使用「{appName} 接口文档」。
     */
    @Bean
    @ConditionalOnMissingBean
    public OpenAPI g2rainOpenApi(
        @Value("${spring.application.name:application}") String appName,
        @Value("${g2rain.springdoc.api-version:1.0}") String apiVersion,
        @Value("${g2rain.springdoc.description:}") String description
    ) {
        String desc = description.isBlank() ? appName + " 接口文档" : description;
        return new OpenAPI().info(new Info()
            .title(appName)
            .version(apiVersion)
            .description(desc)
        );
    }

    /**
     * 隐藏带 {@code @Schema(hidden = true)} 的模型属性（返回 {@code null} 表示不出现在文档中）。
     */
    @Bean
    public PropertyCustomizer hiddenSchemaPropertyCustomizer() {
        return (schema, type) -> shouldHideProperty(type) ? null : schema;
    }

    private static boolean shouldHideProperty(AnnotatedType type) {
        if (Objects.isNull(type)) {
            return false;
        }

        Annotation[] ctx = type.getCtxAnnotations();
        if (Objects.nonNull(ctx)) {
            for (Annotation a : ctx) {
                if (a instanceof Schema s && s.hidden()) {
                    return true;
                }
            }
        }

        String propertyName = type.getPropertyName();
        if (Objects.isNull(propertyName)) {
            return false;
        }

        try {
            Field field = Result.class.getDeclaredField(propertyName);
            Schema ann = field.getAnnotation(Schema.class);
            return Objects.nonNull(ann) && ann.hidden();
        } catch (NoSuchFieldException | SecurityException ignored) {
            return false;
        }
    }
}
