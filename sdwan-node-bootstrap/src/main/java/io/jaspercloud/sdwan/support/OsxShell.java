package io.jaspercloud.sdwan.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class OsxShell {

    private OsxShell() {

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
