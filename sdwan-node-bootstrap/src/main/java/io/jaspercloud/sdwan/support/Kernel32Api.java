package io.jaspercloud.sdwan.support;

import java.util.Arrays;

public final class Kernel32Api {

    private Kernel32Api() {

    }

    public static String GetModuleFileNameA() {
        int size = Short.MAX_VALUE;
        byte[] bytes = new byte[size];
        int len = Kernel32NativeApi.INSTANCE.GetModuleFileNameA(null, bytes, size);
        String path = new String(Arrays.copyOf(bytes, len));
        return path;
    }
}
