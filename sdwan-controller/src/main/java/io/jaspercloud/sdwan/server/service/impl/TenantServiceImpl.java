package io.jaspercloud.sdwan.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.jaspercloud.sdwan.server.controller.request.EditTenantRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.TenantResponse;
import io.jaspercloud.sdwan.server.entity.Node;
import io.jaspercloud.sdwan.server.entity.Tenant;
import io.jaspercloud.sdwan.server.repsitory.TenantMapper;
import io.jaspercloud.sdwan.server.service.NodeService;
import io.jaspercloud.sdwan.server.service.TenantService;
import io.jaspercloud.sdwan.support.ChannelAttributes;
import io.jaspercloud.sdwan.tranport.SdWanServer;
import io.netty.channel.Channel;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class TenantServiceImpl implements TenantService {

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private NodeService nodeService;

    @Resource
    private SdWanServer sdWanServer;

    @Override
    public void add(EditTenantRequest request) {
        Tenant tenant = BeanUtil.toBean(request, Tenant.class);
        tenant.setId(null);
        tenant.insert();
    }

    @Override
    public void edit(EditTenantRequest request) {
        Tenant tenant = BeanUtil.toBean(request, Tenant.class);
        tenant.updateById();
    }

    @Override
    public void del(EditTenantRequest request) {
        tenantMapper.deleteById(request.getId());
    }

    @Override
    public PageResponse<TenantResponse> page() {
        Long total = new LambdaQueryChainWrapper<>(tenantMapper).count();
        List<Tenant> list = new LambdaQueryChainWrapper<>(tenantMapper)
                .list();
        List<TenantResponse> collect = list.stream().map(e -> {
            TenantResponse tenantResponse = BeanUtil.toBean(e, TenantResponse.class);
            List<Node> nodeList = nodeService.queryByTenantId(e.getId());
            tenantResponse.setTotalNode(nodeList.size());
            tenantResponse.setOnlineNode(calcOnlineCount(e, sdWanServer.getOnlineChannel()));
            return tenantResponse;
        }).collect(Collectors.toList());
        PageResponse<TenantResponse> response = PageResponse.build(collect, total, 0L, 0L);
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
