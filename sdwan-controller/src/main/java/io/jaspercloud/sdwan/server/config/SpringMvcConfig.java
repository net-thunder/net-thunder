package io.jaspercloud.sdwan.server.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.fun.SaParamFunction;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import io.jaspercloud.sdwan.server.component.SdWanControllerProperties;
import io.jaspercloud.sdwan.server.controller.response.SessionInfo;
import io.jaspercloud.sdwan.server.enums.UserRole;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SpringMvcConfig implements WebMvcConfigurer {

    @Resource
    private SdWanControllerProperties properties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String storage = properties.getHttpServer().getStorage();
        String location = String.format("file:%s/", storage);
        registry.addResourceHandler("/api/storage/**")
                .addResourceLocations(location);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaTenantInterceptor(handle -> {
                    StpUtil.checkLogin();
                    String token = StpUtil.getTokenValue();
                    SessionInfo sessionInfo = (SessionInfo) StpUtil.getSession().getTokenSign(token).getTag();
                    String tenantId = SaHolder.getRequest().getHeader("X-Tenant-Id");
                    if (UserRole.Root.equals(sessionInfo.getRole())
                            && null != tenantId) {
                        TenantContextHandler.setTenantId(Long.parseLong(tenantId));
                    } else {
                        TenantContextHandler.setTenantId(sessionInfo.getTenantId());
                    }
                    SaRouter.newMatch()
                            .match("/api/tenant/**")
                            .check(r -> StpUtil.checkRole(UserRole.Root.name()))
                            .match("/api/**")
                            .check(r -> StpUtil.checkRoleOr(
                                    UserRole.Root.name(),
                                    UserRole.TenantAdmin.name()
                            ));
                }))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/account/login",
                        "/api/appVersion/lastVersion"
                )
                .excludePathPatterns("/api/file/**")
                .excludePathPatterns("/api/storage/**");
    }

    private static class SaTenantInterceptor extends SaInterceptor {

        public SaTenantInterceptor(SaParamFunction<Object> auth) {
            super(auth);
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
            TenantContextHandler.remove();
        }
    }
}
