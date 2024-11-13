package io.jaspercloud.sdwan.server.config;

import cn.dev33.satoken.fun.SaParamFunction;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import io.jaspercloud.sdwan.server.controller.response.SessionInfo;
import io.jaspercloud.sdwan.server.enums.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SpringMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaTenantInterceptor(handle -> {
                    StpUtil.checkLogin();
                    String token = StpUtil.getTokenValue();
                    SessionInfo sessionInfo = (SessionInfo) StpUtil.getSession().getTokenSign(token).getTag();
                    TenantContextHandler.setTenantId(sessionInfo.getTenantId());
                    SaRouter.newMatch()
                            .match("/tenant/**")
                            .check(r -> StpUtil.checkRole(UserRole.Root.name()))
                            .match("/**")
                            .check(r -> StpUtil.checkRoleOr(
                                    UserRole.Root.name(),
                                    UserRole.TenantAdmin.name()
                            ));
                }))
                .addPathPatterns("/**")
                .excludePathPatterns("/account/login")
                .excludePathPatterns("/index.html", "/static/**");
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
