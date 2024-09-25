package io.jaspercloud.sdwan.support;

import io.jaspercloud.sdwan.tun.osx.OsxNativeApi;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public final class OsxShell {

    private OsxShell() {

    }

    public static String executable() throws Exception {
        String pid = String.valueOf(OsxNativeApi.geteuid());
        Process process = Runtime.getRuntime().exec(new String[]{"sudo", "-p", "\"Password: \"", "-S", pid});
        process.waitFor();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while (null != (line = reader.readLine())) {

            }
        }
        return null;
    }
}
