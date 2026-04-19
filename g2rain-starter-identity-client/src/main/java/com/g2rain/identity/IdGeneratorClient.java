package com.g2rain.identity;


import com.g2rain.common.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign Client 调用 ID 生成服务接口
 * <p>
 * 提供两种类型的唯一 ID 获取：
 * </p>
 * <ul>
 *     <li>雪花 ID：全局唯一，适合分布式系统生成唯一标识</li>
 *     <li>业务 ID：用于业务场景，可根据业务标签生成</li>
 * </ul>
 *
 * @author alpha
 * @since 2025/12/30
 */
@FeignClient(name = "${g2rain.id.generator.service-name:g2rain-infra}", contextId = "idGeneratorClient", url = "${g2rain.id.generator.service-url:}", path = "${g2rain.id.generator.service-path:g2rain_raindrop}")
public interface IdGeneratorClient {

    /**
     * 获取雪花算法生成的全局唯一 ID
     *
     * <p>
     * 使用雪花算法生成，保证在分布式环境下全局唯一。
     * </p>
     *
     * @return {@link Result} 包含生成的 {@link Long} 类型 ID
     */
    @GetMapping(value = "${g2rain.id.generator.path-snowflake:/snowflake}")
    Result<Long> getSnowflakeId();

    /**
     * 获取业务 ID，可根据业务标签生成
     *
     * <p>
     * 适合业务系统使用，可通过 {@code bizTag} 区分不同业务维度。
     * 不依赖底层实现细节。
     * </p>
     *
     * @param bizTag 可选业务标签
     * @return {@link Result} 包含生成的 {@link Long} 类型业务 ID
     */
    @GetMapping(value = "${g2rain.id.generator.path-business:/business}")
    Result<Long> getBusinessId(@RequestParam(value = "bizTag", required = false) String bizTag);
}

