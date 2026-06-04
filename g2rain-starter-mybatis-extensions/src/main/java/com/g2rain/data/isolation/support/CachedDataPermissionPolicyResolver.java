package com.g2rain.data.isolation.support;

import com.g2rain.common.json.JsonCodecFactory;
import com.g2rain.common.model.Result;
import com.g2rain.common.syncer.AbstractMessageStorage;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.data.isolation.DataPermissionPolicyClient;
import com.g2rain.data.isolation.DataPermissionPolicyResolver;
import com.g2rain.data.isolation.model.DataPermissionPolicyResolveResult;
import com.g2rain.data.isolation.model.PermissionPolicyScope;
import com.g2rain.data.isolation.model.PolicyCacheKey;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 带本地缓存的数据权限策略解析器。
 */
@Slf4j
public class CachedDataPermissionPolicyResolver extends AbstractMessageStorage<String, PermissionPolicyScope, String>
    implements DataPermissionPolicyResolver {

    private static final String DATA_SOURCE = "DATA_PERMISSION_POLICY";

    private final Cache<@NonNull PolicyCacheKey, Optional<DataPermissionPolicyResolveResult>> cache = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .maximumSize(50_000)
        .recordStats()
        .build();

    private final DataPermissionPolicyClient dataPermissionPolicyClient;

    public CachedDataPermissionPolicyResolver(DataPermissionPolicyClient dataPermissionPolicyClient) {
        this.dataPermissionPolicyClient = dataPermissionPolicyClient;
    }

    @Override
    public DataPermissionPolicyResolveResult resolve(Long organId, String moduleCode, String tableName) {
        Long userId = PrincipalContextHolder.getUserId();
        String deptPath = PrincipalContextHolder.getDeptPath();
        if (Objects.isNull(userId) || !StringUtils.hasText(deptPath)) {
            return null;
        }

        PolicyCacheKey cacheKey = PolicyCacheKey.of(organId, userId, deptPath, moduleCode, tableName);
        Optional<DataPermissionPolicyResolveResult> cached = cache.getIfPresent(cacheKey);
        if (Objects.nonNull(cached)) {
            return cached.orElse(null);
        }

        Result<DataPermissionPolicyResolveResult> result = dataPermissionPolicyClient.resolveDataPermissionPolicy(
            organId, userId, deptPath, moduleCode, tableName
        );
        if (Objects.isNull(result) || !result.isSuccess()) {
            return null;
        }

        Optional<DataPermissionPolicyResolveResult> resolved = Optional.ofNullable(result.getData());
        cache.put(cacheKey, resolved);
        return resolved.orElse(null);
    }

    @Override
    protected @NonNull String dataSource() {
        return DATA_SOURCE;
    }

    @Override
    protected @NonNull Class<PermissionPolicyScope> getValueType() {
        return PermissionPolicyScope.class;
    }

    @Override
    protected @NonNull String getKey(@NonNull PermissionPolicyScope scope) {
        return scope.getOrganId() + "|" + scope.getLevel();
    }

    @Override
    protected void create(@NonNull String key, PermissionPolicyScope value) {
        update(key, value);
    }

    @Override
    protected void delete(@NonNull String key) {
        // syncer DELETE 仅携带 key，分级失效统一走 UPDATE/CREATE 载荷
    }

    @Override
    protected void update(@NonNull String key, PermissionPolicyScope value) {
        invalidate(value);
    }

    @Override
    protected String get(@NonNull String key) {
        return "";
    }

    private void invalidate(PermissionPolicyScope scope) {
        log.info("收到数据权限策略缓存失效 sync 消息: scope={}", JsonCodecFactory.instance().obj2str(scope));
        if (!PolicyCacheKey.isValidScope(scope)) {
            return;
        }

        cache.asMap().keySet().removeIf(cacheKey -> cacheKey.matches(scope));
    }
}
