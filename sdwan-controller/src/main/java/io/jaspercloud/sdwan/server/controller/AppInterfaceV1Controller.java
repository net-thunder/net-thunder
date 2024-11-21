package io.jaspercloud.sdwan.server.controller;

import io.jaspercloud.sdwan.server.component.SdWanControllerProperties;
import io.jaspercloud.sdwan.server.controller.common.AppResponse;
import io.jaspercloud.sdwan.server.controller.response.AppCheckResponse;
import io.jaspercloud.sdwan.server.entity.AppVersion;
import io.jaspercloud.sdwan.server.service.AppVersionService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class AppInterfaceV1Controller {

    @Resource
    private SdWanControllerProperties properties;

    @Resource
    private AppVersionService appVersionService;

    /**
     * 老版本 获取配置
     *
     * @param tenantId
     * @return
     */
    @GetMapping("/config/controllerConfig")
    public AppResponse controllerConfig(@RequestParam("tenantId") String tenantId) {
        AppResponse appResponse = new AppResponse();
        appResponse.setCode(HttpStatus.OK.value());
        appResponse.setData(properties.getHttpServer().getControllerServer());
        return appResponse;
    }

    /**
     * 老版本 版本更新
     *
     * @param platform
     * @param md5
     * @return
     * @throws Exception
     */
    @GetMapping("/appVersion/check")
    public AppResponse<AppCheckResponse> check(@RequestParam("platform") String platform,
                                               @RequestParam("md5") String md5) throws Exception {
        AppVersion appVersion = appVersionService.queryLastVersion(platform);
        AppResponse<AppCheckResponse> appResponse = new AppResponse<>();
        if (null == appVersion) {
            appResponse.setCode(HttpStatus.NO_CONTENT.value());
            return appResponse;
        }
        appResponse.setCode(HttpStatus.OK.value());
        AppCheckResponse appCheckResponse = new AppCheckResponse();
        appCheckResponse.setPath(String.format("/api/storage/%s", appVersion.getPath()));
        appCheckResponse.setMd5(appVersion.getMd5());
        appResponse.setData(appCheckResponse);
        return appResponse;
    }
}
