package io.jaspercloud.sdwan.server.config;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import io.jaspercloud.sdwan.exception.ProcessException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.commons.lang3.StringUtils;

public class TenantContextHandler implements TenantLineHandler {

    private static ThreadLocal<Long> threadLocal = new TransmittableThreadLocal();

    public static void setTenantId(String tenantId) {
        if (StringUtils.isEmpty(tenantId)) {
            threadLocal.set(null);
        } else {
            threadLocal.set(Long.parseLong(tenantId));
        }
    }

    public static void setTenantId(Long tenantId) {
        threadLocal.set(tenantId);
    }

    public static void remove() {
        threadLocal.remove();
    }

    @Override
    public boolean ignoreTable(String tableName) {
        return "biz_tenant".equals(tableName);
    }

    @Override
    public Expression getTenantId() {
        Long tenantId = threadLocal.get();
        if (null == tenantId) {
            throw new ProcessException("not found tenantId");
        }
        return new LongValue(tenantId);
    }
}
