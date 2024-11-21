package io.jaspercloud.sdwan.server.controller.response;

import io.jaspercloud.sdwan.server.entity.Node;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VNATResponse {

    private Long id;
    private String name;
    private String description;
    private String srcCidr;
    private String dstCidr;
    private List<Node> nodeList;
    private List<Long> groupIdList;
    private Boolean enable;
}
