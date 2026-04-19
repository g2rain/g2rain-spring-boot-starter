package com.g2rain.data.isolation;

import com.g2rain.common.enums.OrganType;
import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.data.isolation.enums.IsolationErrorCode;
import com.g2rain.data.isolation.model.DataIsolationMeta;
import com.g2rain.mybatis.extension.InvocationContext;
import com.g2rain.mybatis.extension.IsolationFieldExtractor;
import com.g2rain.mybatis.extension.UpdateProcessor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;

import java.util.Objects;
import java.util.Set;

/**
 * 数据隔离新增处理器。
 * <p>
 * 仅拦截 INSERT 操作，并在执行前校验目标组织是否在当前组织的数据访问范围内。
 * </p>
 *
 * @author alpha
 * @since 2026/3/8
 */
public class IsolationInsertProcessor extends UpdateProcessor {

    /**
     * 数据范围校验器。
     */
    private final DataScopeExaminer dataScopeExaminer;

    /**
     * 拦截器顺序
     */
    private final int order;

    /**
     * 构造函数。
     *
     * @param dataScopeExaminer 数据范围校验器
     * @param order             拦截器顺序
     */
    public IsolationInsertProcessor(DataScopeExaminer dataScopeExaminer, int order) {
        this.dataScopeExaminer = dataScopeExaminer;
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
        return !PrincipalContextHolder.isAdminCompany();
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

        // 获取 ScopedValue 的调用链上下文操作目标组织标识
        Long targetOrganId = PrincipalContextHolder.getTargetOrganId();

        // 如果是租户直接用租户的组织标识
        if (Objects.isNull(targetOrganId) && OrganType.isTenant(PrincipalContextHolder.getOrganType())) {
            targetOrganId = PrincipalContextHolder.getOrganId();
        }

        // 获取执行 Mapper 接口方法的参数中的目标组织标识
        if (Objects.isNull(targetOrganId)) {
            // 1. 获取configuration
            Configuration configuration = mappedStatement.getConfiguration();

            // 3. 获取数据隔离的实际参数名称
            String propertyName = meta.getOrganIdPropertyName();

            Set<Object> values = IsolationFieldExtractor.extractValues(configuration, parameter, propertyName);
            if (values.size() != 1) {
                throw new BusinessException(IsolationErrorCode.ISOLATION_TENANT_NOT_EXIST, "tenantId");
            }

            Object val = values.iterator().next();
            targetOrganId = (val instanceof Number) ? ((Number) val).longValue() : null;
        }

        if (Objects.isNull(targetOrganId)) {
            throw new BusinessException(IsolationErrorCode.ISOLATION_TENANT_NOT_EXIST, "tenantId");
        }

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
