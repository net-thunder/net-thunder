package io.jaspercloud.sdwan.server.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.config.TenantContextHandler;
import io.jaspercloud.sdwan.server.controller.common.ValidGroup;
import io.jaspercloud.sdwan.server.controller.request.EditTenantRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.TenantResponse;
import io.jaspercloud.sdwan.server.entity.Account;
import io.jaspercloud.sdwan.server.entity.Node;
import io.jaspercloud.sdwan.server.entity.Tenant;
import io.jaspercloud.sdwan.server.service.AccountService;
import io.jaspercloud.sdwan.server.service.NodeService;
import io.jaspercloud.sdwan.server.service.TenantService;
import io.jaspercloud.sdwan.tranport.P2pClient;
import io.jaspercloud.sdwan.tranport.RelayClient;
import io.jaspercloud.sdwan.tranport.ControllerServer;
import io.jaspercloud.sdwan.util.SocketAddressUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/tenant")
public class TenantController {

    @Resource
    private TenantService tenantService;

    @Resource
    private AccountService accountService;

    @Resource
    private NodeService nodeService;

    @Resource
    private ControllerServer controllerServer;

    @PostMapping("/add")
    public void add(@Validated(ValidGroup.Add.class) @RequestBody EditTenantRequest request) {
        request.check();
        checkServerList(request.getStunServerList(), request.getRelayServerList());
        tenantService.add(request);
    }

    @PostMapping("/edit")
    public void edit(@Validated(ValidGroup.Update.class) @RequestBody EditTenantRequest request) {
        request.check();
        checkServerList(request.getStunServerList(), request.getRelayServerList());
        tenantService.edit(request);
    }

    @PostMapping("/del")
    public void del(@Validated(ValidGroup.Delete.class) @RequestBody EditTenantRequest request) {
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
        int online = controllerServer.getOnlineChannel(tenant.getCode());
        tenantResponse.setOnlineNode(online);
        return tenantResponse;
    }

    @GetMapping("/current")
    public TenantResponse current() {
        Long tenantId = TenantContextHandler.getCurrentTenantId();
        Tenant tenant = tenantService.queryById(tenantId);
        if (null == tenant) {
            return null;
        }
        TenantContextHandler.setTenantId(tenant.getId());
        TenantResponse tenantResponse = BeanUtil.toBean(tenant, TenantResponse.class);
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
            int online = controllerServer.getOnlineChannel(e.getCode());
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
            int online = controllerServer.getOnlineChannel(e.getCode());
            tenantResponse.setOnlineNode(online);
            return tenantResponse;
        }).collect(Collectors.toList());
        PageResponse<TenantResponse> response = PageResponse.build(collect, pageResponse.getTotal(), pageResponse.getSize(), pageResponse.getCurrent());
        return response;
    }

    private void checkServerList(List<String> stunServerList, List<String> relayServerList) {
        long timeout = 1500;
        if (CollectionUtil.isNotEmpty(stunServerList)) {
            P2pClient client = new P2pClient();
            client.start();
            try {
                for (String server : stunServerList) {
                    try {
                        InetSocketAddress socketAddress = SocketAddressUtil.parse(server);
                        client.sendBind(socketAddress, timeout).get();
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        throw new ProcessException(String.format("检测%s失败", server));
                    }
                }
            } finally {
                client.stop();
            }
        }
        if (CollectionUtil.isNotEmpty(relayServerList)) {
            RelayClient client = new RelayClient();
            client.start();
            try {
                for (String server : relayServerList) {
                    try {
                        InetSocketAddress socketAddress = SocketAddressUtil.parse(server);
                        client.sendBind(socketAddress, timeout).get();
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        throw new ProcessException(String.format("检测%s失败", server));
                    }
                }
            } finally {
                client.stop();
            }
        }
    }
}
