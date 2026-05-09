package com.g2rain.data.isolation.support;

import com.g2rain.common.syncer.AbstractMessageStorage;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.data.isolation.DataScopeExaminer;
import com.g2rain.data.isolation.model.OrganScope;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 带本地缓存的数据范围校验器。
 * <p>
 * 以“当前组织 + 目标组织”作为缓存键，减少重复远程调用或重复数据库校验。
 * </p>
 *
 * @author alpha
 * @since 2025/10/13
 */
public class CachedDataScopeExaminer extends AbstractMessageStorage<String, OrganScope, String> implements DataScopeExaminer {
    /**
     * 组织范围校验缓存。
     */
    private final Cache<@NonNull String, Boolean> cache = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .maximumSize(50_000)
        .recordStats()
        .build();

    /**
     * 真实校验器（缓存未命中时调用）。
     */
    private final DataScopeExaminer dataScopeExaminer;

    public CachedDataScopeExaminer(DataScopeExaminer dataScopeExaminer) {
        this.dataScopeExaminer = dataScopeExaminer;
    }

    /**
     * 先走缓存，再回退到真实校验器。
     *
     * @param tenantId 目标组织标识
     * @return 是否在访问范围内
     */
    @Override
    public boolean isOrganInScope(Long tenantId) {
        Long organId = PrincipalContextHolder.getOrganId();
        if (Objects.isNull(organId) || Objects.isNull(tenantId)) {
            return false;
        }

        if (organId.equals(tenantId)) {
            return true;
        }

        String cacheKey = String.format("%d_%d", organId, tenantId);
        Boolean cached = cache.getIfPresent(cacheKey);
        if (Objects.nonNull(cached)) {
            return cached;
        }

        boolean tenantInScope = dataScopeExaminer.isOrganInScope(tenantId);
        cache.put(cacheKey, tenantInScope);
        return tenantInScope;
    }

    @Override
    protected @NonNull String dataSource() {
        return "ORGAN_HIERARCHY";
    }

    @Override
    protected @NonNull Class<OrganScope> getValueType() {
        return OrganScope.class;
    }

    @Override
    protected @NonNull String getKey(@NonNull OrganScope value) {
        Long sourceOrganId = value.getSourceOrganId();
        Long targetOrganId = value.getTargetOrganId();
        return String.format("%d_%d", sourceOrganId, targetOrganId);
    }

    @Override
    protected void create(@NonNull String key, OrganScope value) {
        delete(key);
    }

    @Override
    protected void delete(@NonNull String key) {
        cache.invalidate(key);
    }

    @Override
    protected void update(@NonNull String key, OrganScope value) {
        delete(key);
    }

    @Override
    protected String get(@NonNull String key) {
        return "";
    }
}
