package io.jaspercloud.sdwan.server.controller;

import io.jaspercloud.sdwan.server.controller.common.ValidGroup;
import io.jaspercloud.sdwan.server.controller.request.EditAppVersionRequest;
import io.jaspercloud.sdwan.server.entity.AppVersion;
import io.jaspercloud.sdwan.server.service.AppVersionService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/appVersion")
public class AppVersionController {

    @Resource
    private AppVersionService appVersionService;

    @PostMapping("/add")
    public void add(@Validated(ValidGroup.Add.class) @RequestBody EditAppVersionRequest request) {
        appVersionService.add(request);
    }

    @PostMapping("/del")
    public void del(@Validated(ValidGroup.Delete.class) @RequestBody EditAppVersionRequest request) {
        appVersionService.del(request);
    }

    @GetMapping("/list")
    public List<AppVersion> list() {
        List<AppVersion> list = appVersionService.list();
        return list;
    }

    @GetMapping("/lastVersion")
    public Map<String, String> lastVersionList() {
        List<AppVersion> list = appVersionService.lastVersionList();
        Map<String, String> collect = list.stream().collect(Collectors.toMap(e -> e.getPlatform(), e -> e.getPath()));
        return collect;
    }
}
