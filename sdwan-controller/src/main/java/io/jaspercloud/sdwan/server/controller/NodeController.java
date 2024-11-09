package io.jaspercloud.sdwan.server.controller;

import io.jaspercloud.sdwan.server.controller.request.EditNodeRequest;
import io.jaspercloud.sdwan.server.controller.response.NodeDetailResponse;
import io.jaspercloud.sdwan.server.controller.response.NodeResponse;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.service.NodeService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/node")
public class NodeController {

    @Resource
    private NodeService nodeService;

    @PostMapping("/add")
    public void add(@RequestBody EditNodeRequest request) {
        nodeService.add(request);
    }

    @PostMapping("/edit")
    public void edit(@RequestBody EditNodeRequest request) {
        nodeService.add(request);
    }

    @PostMapping("/del")
    public void del(@RequestBody EditNodeRequest request) {
        nodeService.del(request);
    }

    @GetMapping("/detail/{id}")
    public NodeDetailResponse detail(@PathVariable("id") Long id) {
        NodeDetailResponse detail = nodeService.detail(id);
        return detail;
    }

    @GetMapping("/page")
    public PageResponse<NodeResponse> page() {
        PageResponse<NodeResponse> response = nodeService.page();
        return response;
    }
}
