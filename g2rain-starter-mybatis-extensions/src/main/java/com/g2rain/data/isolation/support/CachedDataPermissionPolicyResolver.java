package com.g2rain.data.isolation.support;

import com.g2rain.common.syncer.AbstractMessageStorage;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.data.isolation.DataPermissionPolicyResolver;
import com.g2rain.data.isolation.model.DataPermissionPolicyResolveResult;
import com.g2rain.data.isolation.model.PermissionPolicyScope;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 带本地缓存的数据权限策略解析器。
 */
public class CachedDataPermissionPolicyResolver extends AbstractMessageStorage<String, PermissionPolicyScope, String> implements DataPermissionPolicyResolver {

    private final Cache<@NonNull String, DataPermissionPolicyResolveResult> cache = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .maximumSize(50_000)
        .recordStats()
        .build();

    private final DataPermissionPolicyResolver dataPermissionPolicyResolver;

    public CachedDataPermissionPolicyResolver(DataPermissionPolicyResolver dataPermissionPolicyResolver) {
        this.dataPermissionPolicyResolver = dataPermissionPolicyResolver;
    }

    @Override
    public DataPermissionPolicyResolveResult resolve(Long organId, String moduleCode, String tableName) {
        Long userId = PrincipalContextHolder.getUserId();
        String deptPath = PrincipalContextHolder.getDeptPath();
        if (Objects.isNull(userId) || !StringUtils.hasText(deptPath)) {
            return null;
        }

        String cacheKey = buildCacheKey(organId, userId, deptPath, moduleCode, tableName);
        DataPermissionPolicyResolveResult cached = cache.getIfPresent(cacheKey);
        if (Objects.nonNull(cached)) {
            return cached;
        }

        DataPermissionPolicyResolveResult resolved = dataPermissionPolicyResolver.resolve(organId, moduleCode, tableName);
        if (Objects.nonNull(resolved)) {
            cache.put(cacheKey, resolved);
        }
        return resolved;
    }

    static String buildCacheKey(Long organId, Long userId, String deptPath, String moduleCode, String tableName) {
        return String.format("%d_%d_%s_%s_%s", organId, userId, deptPath, moduleCode, tableName);
    }

    @Override
    protected @NonNull String dataSource() {
        return "DATA_PERMISSION_POLICY";
    }

    @Override
    protected @NonNull Class<PermissionPolicyScope> getValueType() {
        return PermissionPolicyScope.class;
    }

    @Override
    protected @NonNull String getKey(@NonNull PermissionPolicyScope o) {
        return buildCacheKey(o.getOrganId(), o.getUserId(), o.getDeptPaths(), o.getModuleCode(), o.getTableName());
    }

    @Override
    protected void create(@NonNull String key, PermissionPolicyScope value) {
        delete(key);
    }

    @Override
    protected void delete(@NonNull String key) {
        cache.invalidate(key);
    }

    @Override
    protected void update(@NonNull String key, PermissionPolicyScope value) {
        delete(key);
    }

    @Override
    protected String get(@NonNull String key) {
        return "";
    }
}
