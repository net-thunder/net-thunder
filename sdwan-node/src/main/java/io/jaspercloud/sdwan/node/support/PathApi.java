package io.jaspercloud.sdwan.node.support;

import io.jaspercloud.sdwan.tun.osx.OsxNativeApi;
import io.jaspercloud.sdwan.tun.windows.Kernel32NativeApi;
import io.jaspercloud.sdwan.util.GraalVM;
import io.jaspercloud.sdwan.util.Jpackage;
import io.netty.util.internal.PlatformDependent;

import java.io.File;
import java.util.Arrays;

/**
 * @author jasper
 * @create 2024/9/25
 */
public class PathApi {

    public static String getExecutableParent() {
        if (GraalVM.isNative()) {
            if (PlatformDependent.isWindows()) {
                String path = new File(getExecutableFromWin()).getParent();
                return path;
            } else if (PlatformDependent.isOsx()) {
                String path = new File(getExecutableFromOSX()).getParent();
                return path;
            } else {
                String path = System.getProperty("user.dir");
                return path;
            }
        } else if (Jpackage.isJpackage()) {
            String path = new File(System.getProperty("user.dir"), "app").getAbsolutePath();
            return path;
        } else {
            String path = System.getProperty("user.dir");
            return path;
        }
    }

    public static String getExecutableFromWin() {
        int size = Short.MAX_VALUE;
        byte[] bytes = new byte[size];
        int len = Kernel32NativeApi.INSTANCE.GetModuleFileNameA(null, bytes, size);
        String path = new String(Arrays.copyOf(bytes, len));
        return path;
    }

    public static String getExecutableFromOSX() {
        byte[] bytes = new byte[OsxNativeApi.PROC_PIDPATHINFO_MAXSIZE];
        int len = OsxNativeApi.proc_pidpath(OsxNativeApi.getpid(), bytes, bytes.length);
        String path = new String(Arrays.copyOf(bytes, len));
        return path;
    }
}
