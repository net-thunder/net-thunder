package io.jaspercloud.sdwan.server.controller;

import io.jaspercloud.sdwan.server.controller.request.EditNodeRequest;
import io.jaspercloud.sdwan.server.controller.response.NodeDetailResponse;
import io.jaspercloud.sdwan.server.controller.response.NodeResponse;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.service.NodeService;
import io.jaspercloud.sdwan.tranport.SdWanServer;
import io.netty.channel.Channel;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/node")
public class NodeController {

    @Resource
    private NodeService nodeService;

    @Resource
    private SdWanServer sdWanServer;

    @PostMapping("/add")
    public void add(@RequestBody EditNodeRequest request) {
        nodeService.add(request);
    }

    @PostMapping("/edit")
    public void edit(@RequestBody EditNodeRequest request) {
        nodeService.edit(request);
    }

    @PostMapping("/del")
    public void del(@RequestBody EditNodeRequest request) {
        nodeService.del(request);
    }

    @GetMapping("/detail/{id}")
    public NodeDetailResponse detail(@PathVariable("id") Long id) {
        NodeDetailResponse nodeResponse = nodeService.queryDetail(id);
        if (null == nodeResponse) {
            return null;
        }
        Channel channel = sdWanServer.getChannelSpace(String.valueOf(nodeResponse.getTenantId()), nodeResponse.getVip());
        nodeResponse.setOnline(null != channel);
        return nodeResponse;
    }

    @GetMapping("/list")
    public List<NodeResponse> list() {
        List<NodeResponse> list = nodeService.list();
        list.forEach(e -> {
            Channel channel = sdWanServer.getChannelSpace(String.valueOf(e.getTenantId()), e.getVip());
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
