package io.jaspercloud.sdwan.util;

import io.netty.util.internal.PlatformDependent;

public class PlatformUtil {

    public static final String WINDOWS = "windows";
    public static final String MACOS = "macos";

    public static String normalizedOs() {
        if (PlatformDependent.isWindows()) {
            return WINDOWS;
        } else if (PlatformDependent.isOsx()) {
            return MACOS;
        } else {
            return PlatformDependent.normalizedOs();
        }
    }
}
