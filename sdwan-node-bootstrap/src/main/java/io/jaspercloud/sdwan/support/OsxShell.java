package io.jaspercloud.sdwan.support;

import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.tun.osx.OsxNativeApi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class OsxShell {

    private OsxShell() {

    }

    public static String executable() throws Exception {
        String pid = String.valueOf(OsxNativeApi.getpid());
        List<String> list = new ArrayList<>(Arrays.asList("bash", "-c"));
        String cmd = String.format("ps -p %s", pid);
        list.add(cmd);
        Process process = new ProcessBuilder(list)
                .redirectErrorStream(true)
                .start();
        try {
            process.waitFor();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while (null != (line = reader.readLine())) {
                    if (line.contains(pid)) {
                        int s = line.indexOf("/");
                        int e = line.indexOf(" ", s);
                        if (e == -1) {
                            e = line.length();
                        }
                        String path = line.substring(s, e);
                        return path;
                    }
                }
            }
        } finally {
            process.destroy();
        }
        throw new ProcessException("not found executable");
    }

    public static void execute(String path, String[] args) throws Exception {
        List<String> list = new ArrayList<>(Arrays.asList("bash", "-c"));
        list.add(path);
        list.addAll(Arrays.asList(args));
        Process process = new ProcessBuilder(list)
                .redirectErrorStream(true)
                .start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while (null != (line = reader.readLine())) {
                System.out.println(line);
            }
            process.waitFor();
        } finally {
            process.destroy();
        }
    }
}
