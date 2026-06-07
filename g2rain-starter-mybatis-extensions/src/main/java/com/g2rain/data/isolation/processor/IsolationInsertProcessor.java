package com.g2rain.data.isolation.processor;

import com.g2rain.common.enums.OrganType;
import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.data.isolation.DataIsolationCache;
import com.g2rain.data.isolation.DataScopeExaminer;
import com.g2rain.data.isolation.enums.IsolationErrorCode;
import com.g2rain.data.isolation.model.DataIsolationMeta;
import com.g2rain.data.isolation.model.DataPermissionPolicyResolveResult;
import com.g2rain.data.isolation.support.CachedDataPermissionPolicyResolver;
import com.g2rain.data.isolation.util.IsolationOrganSupport;
import com.g2rain.mybatis.extension.InvocationContext;
import com.g2rain.mybatis.extension.UpdateProcessor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;

import java.util.Objects;

/**
 * 数据隔离新增处理器。
 * <p>
 * 仅拦截 INSERT 操作，并在执行前校验目标组织是否在当前组织的数据访问范围内，
 * 并在配置动态策略时校验写入权限。
 * </p>
 *
 * @author alpha
 * @since 2026/3/8
 */
@Deprecated
public class IsolationInsertProcessor extends UpdateProcessor {

    /**
     * 数据范围校验器。
     */
    private final DataScopeExaminer dataScopeExaminer;

    /**
     * 数据权限策略解析器。
     */
    private final CachedDataPermissionPolicyResolver dataPermissionPolicyResolver;

    /**
     * 拦截器顺序
     */
    private final int order;

    /**
     * 构造函数。
     *
     * @param dataScopeExaminer            数据范围校验器
     * @param dataPermissionPolicyResolver 数据权限策略解析器
     * @param order                        拦截器顺序
     */
    public IsolationInsertProcessor(DataScopeExaminer dataScopeExaminer, CachedDataPermissionPolicyResolver dataPermissionPolicyResolver, int order) {
        this.dataScopeExaminer = dataScopeExaminer;
        this.dataPermissionPolicyResolver = dataPermissionPolicyResolver;
        this.order = order;
    }

    /**
     * 判断当前调用是否需要执行隔离拦截。
     *
     * @param context 调用上下文
     * @return 是否拦截
     */
    @Override
    public boolean shouldIntercept(InvocationContext context) {
        // 后端发起请求不进行拦截
        if (PrincipalContextHolder.isBackEnd()) {
            return false;
        }

        // 运营公司不进行拦截
        if (PrincipalContextHolder.isAdminCompany()) {
            return false;
        }

        return OrganType.isTenant(PrincipalContextHolder.getOrganType());
    }

    @Override
    protected void onUpdate(Executor executor, MappedStatement mappedStatement, Object parameter) {
        SqlCommandType sct = mappedStatement.getSqlCommandType();
        // 拦截新增数据操作
        if (sct != SqlCommandType.INSERT) {
            return;
        }

        // 拦截被数据隔离注解标识的接口, 并且没有设置忽略隔离的方法
        DataIsolationMeta meta = DataIsolationCache.getMeta(mappedStatement.getId());
        if (Objects.isNull(meta)) {
            return;
        }

        Long targetOrganId = IsolationOrganSupport.resolveTargetOrganId();
        if (!dataScopeExaminer.isOrganInScope(targetOrganId)) {
            throw new BusinessException(IsolationErrorCode.ISOLATION_TENANT_NON_SCOPE, targetOrganId);
        }
    }

    /**
     * 返回处理器执行顺序。
     *
     * @return 顺序值
     */
    @Override
    public int order() {
        return this.order;
    }
}
