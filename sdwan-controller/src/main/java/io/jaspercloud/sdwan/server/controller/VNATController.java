package io.jaspercloud.sdwan.server.controller;

import cn.hutool.core.bean.BeanUtil;
import io.jaspercloud.sdwan.server.controller.common.ValidGroup;
import io.jaspercloud.sdwan.server.controller.request.EditVNATRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.VNATResponse;
import io.jaspercloud.sdwan.server.entity.Node;
import io.jaspercloud.sdwan.server.entity.VNAT;
import io.jaspercloud.sdwan.server.service.GroupConfigService;
import io.jaspercloud.sdwan.server.service.NodeService;
import io.jaspercloud.sdwan.server.service.VNATService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vnat")
public class VNATController extends BaseController {

    @Resource
    private VNATService vnatService;

    @Resource
    private NodeService nodeService;

    @Resource
    private GroupConfigService groupConfigService;

    @PostMapping("/add")
    public void add(@Validated(ValidGroup.Add.class) @RequestBody EditVNATRequest request) {
        request.check();
        vnatService.add(request);
        reloadClientList();
    }

    @PostMapping("/edit")
    public void edit(@Validated(ValidGroup.Update.class) @RequestBody EditVNATRequest request) {
        request.check();
        vnatService.edit(request);
        reloadClientList();
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
        VNATResponse response = BeanUtil.toBean(vnat, VNATResponse.class);
        List<Node> nodeList = nodeService.queryByIdList(vnat.getNodeIdList());
        response.setNodeList(nodeList);
        return response;
    }

    @GetMapping("/list")
    public List<VNATResponse> list() {
        List<VNAT> list = vnatService.list();
        List<VNATResponse> collect = list.stream().map(e -> {
            VNATResponse item = BeanUtil.toBean(e, VNATResponse.class);
            List<Node> nodeList = nodeService.queryByIdList(e.getNodeIdList());
            item.setNodeList(nodeList);
            return item;
        }).collect(Collectors.toList());
        return collect;
    }

    @GetMapping("/page")
    public PageResponse<VNATResponse> page() {
        PageResponse<VNAT> pageResponse = vnatService.page();
        List<VNATResponse> collect = pageResponse.getData().stream().map(e -> {
            VNATResponse item = BeanUtil.toBean(e, VNATResponse.class);
            List<Node> nodeList = nodeService.queryByIdList(e.getNodeIdList());
            item.setNodeList(nodeList);
            return item;
        }).collect(Collectors.toList());
        PageResponse<VNATResponse> response = PageResponse.build(collect, pageResponse.getTotal(), pageResponse.getSize(), pageResponse.getCurrent());
        return response;
    }
}
