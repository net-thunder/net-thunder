package io.jaspercloud.sdwan.server.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Node extends TenantEntity<Node> {

    private Long id;
    private String mac;
    private String ip;
    private String vip;
    private Boolean enable;
}
