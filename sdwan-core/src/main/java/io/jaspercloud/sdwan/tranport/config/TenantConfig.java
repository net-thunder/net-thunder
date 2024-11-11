package io.jaspercloud.sdwan.tranport.config;

import io.jaspercloud.sdwan.support.Cidr;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TenantConfig {

    private Cidr ipPool;
    private List<String> stunServerList;
    private List<String> relayServerList;
}
