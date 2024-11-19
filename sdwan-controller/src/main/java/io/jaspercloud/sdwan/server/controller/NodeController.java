package io.jaspercloud.sdwan.server.controller;

import io.jaspercloud.sdwan.server.controller.common.ValidGroup;
import io.jaspercloud.sdwan.server.controller.request.EditNodeRequest;
import io.jaspercloud.sdwan.server.controller.response.NodeDetailResponse;
import io.jaspercloud.sdwan.server.controller.response.NodeResponse;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.entity.Tenant;
import io.jaspercloud.sdwan.server.service.NodeService;
import io.jaspercloud.sdwan.server.service.TenantService;
import io.jaspercloud.sdwan.tranport.SdWanServer;
import io.netty.channel.Channel;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.InetSocketAddress;
import java.util.List;

@RestController
@RequestMapping("/api/node")
public class NodeController {

    @Resource
    private NodeService nodeService;

    @Resource
    private TenantService tenantService;

    @Resource
    private SdWanServer sdWanServer;

    @PostMapping("/add")
    public void add(@Validated(ValidGroup.Add.class) @RequestBody EditNodeRequest request) {
        nodeService.add(request);
    }

    @PostMapping("/edit")
    public void edit(@Validated(ValidGroup.Update.class) @RequestBody EditNodeRequest request) {
        nodeService.edit(request);
    }

    @PostMapping("/del")
    public void del(@Validated(ValidGroup.Delete.class) @RequestBody EditNodeRequest request) {
        nodeService.del(request);
    }

    @GetMapping("/detail/{id}")
    public NodeDetailResponse detail(@PathVariable("id") Long id) {
        NodeDetailResponse nodeResponse = nodeService.queryDetail(id);
        if (null == nodeResponse) {
            return null;
        }
        Tenant tenant = tenantService.queryById(nodeResponse.getTenantId());
        Channel channel = sdWanServer.getChannelSpace(tenant.getCode(), nodeResponse.getVip());
        if (null != channel) {
            InetSocketAddress remotedAddress = (InetSocketAddress) channel.remoteAddress();
            nodeResponse.setIp(remotedAddress.getHostString());
        }
        nodeResponse.setOnline(null != channel);
        return nodeResponse;
    }

    @GetMapping("/list")
    public List<NodeResponse> list() {
        List<NodeResponse> list = nodeService.list();
        list.forEach(e -> {
            Tenant tenant = tenantService.queryById(e.getTenantId());
            Channel channel = sdWanServer.getChannelSpace(tenant.getCode(), e.getVip());
            if (null != channel) {
                InetSocketAddress remotedAddress = (InetSocketAddress) channel.remoteAddress();
                e.setIp(remotedAddress.getHostString());
            }
            e.setOnline(null != channel);
        });
        return list;
    }

    @GetMapping("/page")
    public PageResponse<NodeResponse> page() {
        PageResponse<NodeResponse> response = nodeService.page();
        return response;
    }
}
