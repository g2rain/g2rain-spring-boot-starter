package com.g2rain.identity;


import com.g2rain.common.id.IdGenerator;
import com.g2rain.common.model.Result;
import feign.FeignException;

import java.util.Objects;
import java.util.function.Function;

/**
 * @author alpha
 * @since 2025/12/30
 */
public class IdGeneratorImpl implements IdGenerator {
    private final IdGeneratorClient idGeneratorClient;

    public IdGeneratorImpl(IdGeneratorClient idGeneratorClient) {
        this.idGeneratorClient = idGeneratorClient;
    }

    @Override
    public Long generateId() {
        return safeGeneratorId(idGeneratorClient::getBusinessId, null);
    }

    @Override
    public Long generateId(String bizTag) {
        return safeGeneratorId(idGeneratorClient::getBusinessId, bizTag);
    }

    @Override
    public Long generateSnowflakeId() {
        return safeGeneratorId(biz -> idGeneratorClient.getSnowflakeId(), null);
    }

    private Long safeGeneratorId(Function<String, Result<Long>> function, String bizTag) {
        try {
            Result<Long> result = function.apply(bizTag);
            if (Objects.isNull(result) || !result.isSuccess() || Objects.isNull(result.getData())) {
                return -1L;
            }
            return result.getData();
        } catch (FeignException e) {
            return -1L;
        }
    }
}
