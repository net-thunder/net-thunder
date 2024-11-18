package io.jaspercloud.sdwan.server.controller;

import cn.hutool.core.bean.BeanUtil;
import io.jaspercloud.sdwan.server.config.TenantContextHandler;
import io.jaspercloud.sdwan.server.controller.request.EditTenantRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.TenantResponse;
import io.jaspercloud.sdwan.server.entity.Account;
import io.jaspercloud.sdwan.server.entity.Node;
import io.jaspercloud.sdwan.server.entity.Tenant;
import io.jaspercloud.sdwan.server.service.AccountService;
import io.jaspercloud.sdwan.server.service.NodeService;
import io.jaspercloud.sdwan.server.service.TenantService;
import io.jaspercloud.sdwan.tranport.SdWanServer;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tenant")
public class TenantController {

    @Resource
    private TenantService tenantService;

    @Resource
    private AccountService accountService;

    @Resource
    private NodeService nodeService;

    @Resource
    private SdWanServer sdWanServer;

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

    @GetMapping("/detail/{id}")
    public TenantResponse detail(@PathVariable("id") Long id) {
        Tenant tenant = tenantService.queryById(id);
        if (null == tenant) {
            return null;
        }
        TenantContextHandler.setTenantId(tenant.getId());
        TenantResponse tenantResponse = BeanUtil.toBean(tenant, TenantResponse.class);
        Account account = accountService.queryByTenantId(tenant.getId());
        tenantResponse.setUsername(account.getUsername());
        List<Node> nodeList = nodeService.queryByTenantId(tenant.getId());
        tenantResponse.setTotalNode(nodeList.size());
        int online = sdWanServer.getOnlineChannel(tenant.getCode());
        tenantResponse.setOnlineNode(online);
        return tenantResponse;
    }

    @GetMapping("/list")
    public List<TenantResponse> list() {
        List<Tenant> list = tenantService.list();
        List<TenantResponse> collect = list.stream().map(e -> {
            TenantResponse tenantResponse = BeanUtil.toBean(e, TenantResponse.class);
            TenantContextHandler.setTenantId(e.getId());
            List<Node> nodeList = nodeService.queryByTenantId(e.getId());
            tenantResponse.setTotalNode(nodeList.size());
            int online = sdWanServer.getOnlineChannel(e.getCode());
            tenantResponse.setOnlineNode(online);
            return tenantResponse;
        }).collect(Collectors.toList());
        return collect;
    }

    @GetMapping("/page")
    public PageResponse<TenantResponse> page() {
        PageResponse<Tenant> pageResponse = tenantService.page();
        List<Tenant> list = pageResponse.getData();
        List<TenantResponse> collect = list.stream().map(e -> {
            TenantResponse tenantResponse = BeanUtil.toBean(e, TenantResponse.class);
            TenantContextHandler.setTenantId(e.getId());
            List<Node> nodeList = nodeService.queryByTenantId(e.getId());
            tenantResponse.setTotalNode(nodeList.size());
            int online = sdWanServer.getOnlineChannel(e.getCode());
            tenantResponse.setOnlineNode(online);
            return tenantResponse;
        }).collect(Collectors.toList());
        PageResponse<TenantResponse> response = PageResponse.build(collect, pageResponse.getTotal(), pageResponse.getSize(), pageResponse.getCurrent());
        return response;
    }
}
