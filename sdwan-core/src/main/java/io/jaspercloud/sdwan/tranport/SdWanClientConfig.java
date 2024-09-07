package io.jaspercloud.sdwan.tranport;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @author jasper
 * @create 2024/7/2
 */
@Builder
@Getter
@Setter
public class SdWanClientConfig {

    private String controllerServer;
    private String tenantId;
    private int connectTimeout;
    private long heartTime;
}
