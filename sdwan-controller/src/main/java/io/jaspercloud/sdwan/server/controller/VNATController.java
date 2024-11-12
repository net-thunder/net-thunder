package io.jaspercloud.sdwan.server.controller;

import io.jaspercloud.sdwan.server.controller.request.EditVNATRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.VNATResponse;
import io.jaspercloud.sdwan.server.service.GroupConfigService;
import io.jaspercloud.sdwan.server.service.VNATService;
import jakarta.annotation.Resource;
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
    public void add(@RequestBody EditVNATRequest request) {
        vnatService.add(request);
    }

    @PostMapping("/edit")
    public void edit(@RequestBody EditVNATRequest request) {
        vnatService.edit(request);
    }

    @PostMapping("/del")
    public void del(@RequestBody EditVNATRequest request) {
        vnatService.del(request);
    }

    @GetMapping("/page")
    public PageResponse<VNATResponse> page() {
        PageResponse<VNATResponse> response = vnatService.page();
        return response;
    }

    @PostMapping("/updateConfigList")
    public void updateConfigList(@RequestBody EditVNATRequest request) {
        Long id = request.getId();
        List<Long> groupIdList = request.getGroupIdList();
        groupConfigService.updateGroupVNAT(id, groupIdList);
    }

    @GetMapping("/configList/{id}")
    public List<Long> configList(@PathVariable("id") Long id) {
        List<Long> list = groupConfigService.queryGroupVNATList(id);
        return list;
    }
}
