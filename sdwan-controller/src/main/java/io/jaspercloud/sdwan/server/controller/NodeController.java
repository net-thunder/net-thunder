package io.jaspercloud.sdwan.server.controller;

import cn.hutool.core.collection.CollectionUtil;
import io.jaspercloud.sdwan.server.config.TenantContextHandler;
import io.jaspercloud.sdwan.server.controller.common.ValidGroup;
import io.jaspercloud.sdwan.server.controller.request.EditNodeRequest;
import io.jaspercloud.sdwan.server.controller.request.TestIpRequest;
import io.jaspercloud.sdwan.server.controller.response.ICEAddress;
import io.jaspercloud.sdwan.server.controller.response.NodeDetailResponse;
import io.jaspercloud.sdwan.server.controller.response.NodeResponse;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.entity.Node;
import io.jaspercloud.sdwan.server.entity.Tenant;
import io.jaspercloud.sdwan.server.entity.dto.IpRouteTest;
import io.jaspercloud.sdwan.server.service.NodeService;
import io.jaspercloud.sdwan.server.service.TenantService;
import io.jaspercloud.sdwan.support.AddressUri;
import io.jaspercloud.sdwan.support.ChannelAttributes;
import io.jaspercloud.sdwan.tranport.SdWanServer;
import io.jaspercloud.sdwan.util.AddressType;
import io.netty.channel.Channel;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/node")
public class NodeController extends BaseController {

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
        Node node = nodeService.queryById(request.getId());
        if (null == node.getVip()) {
            return;
        }
        reloadClient(node.getVip());
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
        {
            Channel channel = sdWanServer.getChannelSpace(tenant.getCode(), nodeResponse.getVip());
            if (null != channel) {
                ChannelAttributes attr = ChannelAttributes.attr(channel);
                InetSocketAddress remotedAddress = (InetSocketAddress) channel.remoteAddress();
                nodeResponse.setIp(remotedAddress.getHostString());
                nodeResponse.setOs(attr.getOs());
                nodeResponse.setOsVersion(attr.getOsVersion());
                nodeResponse.setNodeVersion(attr.getNodeVersion());
                List<String> addressUriList = attr.getAddressUriList();
                if (CollectionUtil.isNotEmpty(addressUriList)) {
                    List<ICEAddress> collect = addressUriList.stream().map(e -> {
                        AddressUri addressUri = AddressUri.parse(e);
                        ICEAddress address = new ICEAddress();
                        address.setType(addressUri.getScheme());
                        if (AddressType.RELAY.equals(addressUri.getScheme())) {
                            address.setInfo(addressUri.getParams().get("token"));
                        } else {
                            address.setInfo(String.format("%s:%d", addressUri.getHost(), addressUri.getPort()));
                        }
                        address.setProvider(addressUri.getParams().get("server"));
                        return address;
                    }).collect(Collectors.toList());
                    nodeResponse.setAddressList(collect);
                }
            }
            nodeResponse.setOnline(null != channel);
        }
        nodeResponse.getNodeList().forEach(e -> {
            Channel channel = sdWanServer.getChannelSpace(tenant.getCode(), e.getVip());
            e.setOnline(null != channel);
        });
        return nodeResponse;
    }

    @GetMapping("/list")
    public List<NodeResponse> list() {
        List<NodeResponse> list = nodeService.list();
        list.forEach(e -> {
            Tenant tenant = tenantService.queryById(e.getTenantId());
            Channel channel = sdWanServer.getChannelSpace(tenant.getCode(), e.getVip());
            if (null != channel) {
                ChannelAttributes attr = ChannelAttributes.attr(channel);
                InetSocketAddress remotedAddress = (InetSocketAddress) channel.remoteAddress();
                e.setIp(remotedAddress.getHostString());
                e.setOs(attr.getOs());
                e.setOsVersion(attr.getOsVersion());
                e.setNodeVersion(attr.getNodeVersion());
            }
            e.setOnline(null != channel);
        });
        return list;
    }

    @PostMapping("/test")
    public List<IpRouteTest.Message> test(@Validated @RequestBody TestIpRequest request) {
        Tenant tenant = tenantService.queryById(TenantContextHandler.getCurrentTenantId());
        NodeDetailResponse detail = nodeService.queryDetail(request.getNodeId());
        IpRouteTest ipRouteTest = new IpRouteTest();
        ipRouteTest.setSrcIp(detail.getVip());
        ipRouteTest.setDstIp(request.getIp());
        ipRouteTest.test(tenant, detail);
        return ipRouteTest.getLogList();
    }

    @GetMapping("/page")
    public PageResponse<NodeResponse> page() {
        PageResponse<NodeResponse> response = nodeService.page();
        return response;
    }
}
