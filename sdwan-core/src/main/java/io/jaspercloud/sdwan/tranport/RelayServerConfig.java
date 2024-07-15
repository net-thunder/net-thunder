package io.jaspercloud.sdwan.tranport;

import lombok.*;

/**
 * @author jasper
 * @create 2024/7/5
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RelayServerConfig {

    private int bindPort = 2478;
    private long heartTimeout = 15 * 1000;
}
