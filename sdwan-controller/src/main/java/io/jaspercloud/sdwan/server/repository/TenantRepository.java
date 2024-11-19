package io.jaspercloud.sdwan.server.repository;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.jaspercloud.sdwan.server.entity.Tenant;
import io.jaspercloud.sdwan.server.repository.base.BaseRepositoryImpl;
import io.jaspercloud.sdwan.server.repository.mapper.TenantMapper;
import io.jaspercloud.sdwan.server.repository.po.TenantPO;
import io.jaspercloud.sdwan.server.support.BeanTransformer;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TenantRepository extends BaseRepositoryImpl<Tenant, TenantPO, TenantMapper> {

    @Override
    protected BeanTransformer.Builder<Tenant, TenantPO> transformerBuilder() {
        return super.transformerBuilder()
                .merge((l, h) -> {
                    JSONObject jsonObject = JSONUtil.parseObj(l.getConfig());
                    List<String> stunServerList = jsonObject.getJSONArray("stunServerList").toList(String.class);
                    h.setStunServerList(stunServerList);
                    List<String> relayServerList = jsonObject.getJSONArray("relayServerList").toList(String.class);
                    h.setRelayServerList(relayServerList);
                }, (h, l) -> {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.set("stunServerList", h.getStunServerList());
                    jsonObject.set("relayServerList", h.getRelayServerList());
                    l.setConfig(jsonObject.toString());
                });
    }
}
