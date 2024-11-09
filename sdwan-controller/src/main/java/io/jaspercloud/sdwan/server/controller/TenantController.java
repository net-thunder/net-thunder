package io.jaspercloud.sdwan.server.controller;

import io.jaspercloud.sdwan.server.controller.request.EditTenantRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.TenantResponse;
import io.jaspercloud.sdwan.server.service.TenantService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenant")
public class TenantController {

    @Resource
    private TenantService tenantService;

    @PostMapping("/add")
    public void add(@RequestBody EditTenantRequest request) {
        tenantService.add(request);
    }

    @PostMapping("/edit")
    public void edit(@RequestBody EditTenantRequest request) {
        tenantService.edit(request);
    }

    @PostMapping("/del")
    public void del(@RequestBody EditTenantRequest request) {
        tenantService.del(request);
    }

    @GetMapping("/page")
    public PageResponse<TenantResponse> page() {
        PageResponse<TenantResponse> response = tenantService.page();
        return response;
    }
}
