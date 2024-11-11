package io.jaspercloud.sdwan.server.controller;

import cn.hutool.core.bean.BeanUtil;
import io.jaspercloud.sdwan.server.config.TenantContextHandler;
import io.jaspercloud.sdwan.server.controller.request.EditTenantRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.TenantResponse;
import io.jaspercloud.sdwan.server.entity.Node;
import io.jaspercloud.sdwan.server.entity.Tenant;
import io.jaspercloud.sdwan.server.service.NodeService;
import io.jaspercloud.sdwan.server.service.TenantService;
import io.jaspercloud.sdwan.support.ChannelAttributes;
import io.jaspercloud.sdwan.tranport.SdWanServer;
import io.netty.channel.Channel;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tenant")
public class TenantController {

    @Resource
    private TenantService tenantService;

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

    @GetMapping("/page")
    public PageResponse<TenantResponse> page() {
        PageResponse<Tenant> pageResponse = tenantService.page();
        List<Tenant> list = pageResponse.getData();
        List<TenantResponse> collect = list.stream().map(e -> {
            TenantResponse tenantResponse = BeanUtil.toBean(e, TenantResponse.class);
            TenantContextHandler.setTenantId(e.getId());
            List<Node> nodeList = nodeService.queryByTenantId(e.getId());
            tenantResponse.setTotalNode(nodeList.size());
            tenantResponse.setOnlineNode(calcOnlineCount(e, sdWanServer.getOnlineChannel()));
            return tenantResponse;
        }).collect(Collectors.toList());
        PageResponse<TenantResponse> response = PageResponse.build(collect, pageResponse.getTotal(), pageResponse.getSize(), pageResponse.getCurrent());
        return response;
    }

    private int calcOnlineCount(Tenant tenant, Set<Channel> channelSet) {
        int count = 0;
        for (Channel channel : channelSet) {
            ChannelAttributes attr = ChannelAttributes.attr(channel);
            if (StringUtils.equals(attr.getTenantId(), tenant.getCode())) {
                count++;
            }
        }
        return count;
    }
}
