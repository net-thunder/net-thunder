package io.jaspercloud.sdwan.server.config;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import io.jaspercloud.sdwan.exception.ProcessException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

import java.util.Arrays;
import java.util.List;

public class TenantContextHandler implements TenantLineHandler {

    private static ThreadLocal<Long> threadLocal = new TransmittableThreadLocal();

    private static List<String> IgnoreTableList = Arrays.asList(
            "biz_tenant",
            "biz_account",
            "biz_app_version"
    );

    public static void setTenantId(Long tenantId) {
        threadLocal.set(tenantId);
    }

    public static Long getCurrentTenantId() {
        return threadLocal.get();
    }

    public static void remove() {
        threadLocal.remove();
    }

    @Override
    public boolean ignoreTable(String tableName) {
        return IgnoreTableList.contains(tableName);
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
