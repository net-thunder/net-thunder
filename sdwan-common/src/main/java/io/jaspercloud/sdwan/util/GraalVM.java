package io.jaspercloud.sdwan.util;

/**
 * @author jasper
 * @create 2024/9/25
 */
public final class GraalVM {

    private GraalVM() {

    }

    public static boolean isNative() {
        String kind = System.getProperties().getProperty("org.graalvm.nativeimage.kind");
        if ("executable".equals(kind)) {
            return true;
        } else {
            return false;
        }
    }
}
