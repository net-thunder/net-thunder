package io.jaspercloud.sdwan.support;

import io.jaspercloud.sdwan.exception.ProcessException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClientVersion {

    public static final String NodeVersion = encode(1, 0, 4);

    public static long parseLong(String version) {
        List<Long> collect = Arrays.asList(version.split("\\."))
                .stream()
                .map(e -> Long.parseLong(e))
                .collect(Collectors.toList());
        long v1 = collect.get(0) * 10000L;
        long v2 = collect.get(1) * 100L;
        long v3 = collect.get(2) * 1L;
        long v = v1 + v2 + v3;
        return v;
    }

    public static String encode(long v1, long v2, long v3) {
        if (!(v2 >= 0 && v2 <= 99)) {
            throw new ProcessException("v2: " + v2);
        }
        if (!(v3 >= 0 && v3 <= 99)) {
            throw new ProcessException("v3: " + v3);
        }
        String version = String.format("%s.%s.%s", v1, v2, v3);
        return version;
    }
}
