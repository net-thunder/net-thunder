package io.jaspercloud.sdwan.support;

import lombok.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class AddressUri {

    private String scheme;
    private String host;
    private int port;
    private Map<String, String> params;

    public static AddressUri parse(String addressUri) {
        URI uri = URI.create(addressUri);
        String scheme = uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort();
        Map<String, String> params = new HashMap<>();
        if (null != uri.getQuery()) {
            String[] split = uri.getQuery().split("&");
            for (String sp : split) {
                String[] param = sp.split("=");
                if (param.length == 1) {
                    params.put(param[0], null);
                } else {
                    params.put(param[0], param[1]);
                }
            }
        }
        AddressUri result = new AddressUri();
        result.scheme = scheme;
        result.host = host;
        result.port = port;
        result.params = params;
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(scheme).append("://").append(host).append(":").append(port);
        if (null != params && params.size() > 0) {
            builder.append("?");
            for (Map.Entry<String, String> entry : params.entrySet()) {
                builder.append(entry.getKey()).append("=").append(null == entry.getValue() ? "" : entry.getValue());
                builder.append("&");
            }
            builder.deleteCharAt(builder.length() - 1);
        }
        String uri = builder.toString();
        return uri;
    }
}
