package io.jaspercloud.sdwan.server.controller;

import cn.hutool.core.bean.BeanUtil;
import io.jaspercloud.sdwan.server.controller.common.ValidGroup;
import io.jaspercloud.sdwan.server.controller.request.EditVNATRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.VNATResponse;
import io.jaspercloud.sdwan.server.entity.VNAT;
import io.jaspercloud.sdwan.server.service.GroupConfigService;
import io.jaspercloud.sdwan.server.service.VNATService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vnat")
public class VNATController {

    @Resource
    private VNATService vnatService;

    @Resource
    private GroupConfigService groupConfigService;

    @PostMapping("/add")
    public void add(@Validated(ValidGroup.Add.class) @RequestBody EditVNATRequest request) {
        request.check();
        vnatService.add(request);
    }

    @PostMapping("/edit")
    public void edit(@Validated(ValidGroup.Update.class) @RequestBody EditVNATRequest request) {
        request.check();
        vnatService.edit(request);
    }

    @PostMapping("/del")
    public void del(@Validated(ValidGroup.Delete.class) @RequestBody EditVNATRequest request) {
        vnatService.del(request);
    }

    @GetMapping("/detail/{id}")
    public VNATResponse detail(@PathVariable("id") Long id) {
        VNAT vnat = vnatService.queryDetailById(id);
        if (null == vnat) {
            return null;
        }
        VNATResponse routeResponse = BeanUtil.toBean(vnat, VNATResponse.class);
        return routeResponse;
    }

    @GetMapping("/list")
    public List<VNATResponse> list() {
        List<VNATResponse> list = vnatService.list();
        return list;
    }

    @GetMapping("/page")
    public PageResponse<VNATResponse> page() {
        PageResponse<VNATResponse> response = vnatService.page();
        return response;
    }
}
