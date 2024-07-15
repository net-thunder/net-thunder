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

    private String bindHost = "127.0.0.1";
    private int bindPort = 3478;
}
