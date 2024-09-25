package io.jaspercloud.sdwan.support;

import io.jaspercloud.sdwan.tun.osx.OsxNativeApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class OsxShell {

    private OsxShell() {

    }

    public static String executable() throws Exception {
        byte[] bytes = new byte[OsxNativeApi.PROC_PIDPATHINFO_MAXSIZE];
        int len = OsxNativeApi.proc_pidpath(OsxNativeApi.getpid(), bytes, bytes.length);
        String path = new String(Arrays.copyOf(bytes, len));
        return path;
    }

    public static void executeWaitFor(String path, String[] args) throws Exception {
        List<String> list = new ArrayList<>(Arrays.asList("bash", "-c"));
        list.add(String.format("sudo -S %s", path));
        list.addAll(Arrays.asList(args));
        Process process = new ProcessBuilder(list)
                .inheritIO()
                .start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                process.destroy();
            }
        });
        process.waitFor();
    }
}
