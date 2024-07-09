package io.jaspercloud.sdwan.tranport;

import lombok.*;

/**
 * @author jasper
 * @create 2024/7/2
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class StunServerConfig {

    private String bindHost;
    private int bindPort;
}
