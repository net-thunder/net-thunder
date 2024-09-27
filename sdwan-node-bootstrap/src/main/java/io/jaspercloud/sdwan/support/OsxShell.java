package io.jaspercloud.sdwan.support;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class OsxShell {

    private OsxShell() {

    }

    public static void execute(String path, String[] args) throws Exception {
        List<String> list = new ArrayList<>(Arrays.asList("bash", "-c"));
        String format = "osascript -e 'do shell script \"%s %s\" with administrator privileges'";
        String cmd = String.format(format, path, StringUtils.join(args, " "));
        list.add(cmd);
        Process process = new ProcessBuilder(list)
                .inheritIO()
                .start();
        try {
            process.waitFor();
        } finally {
            process.destroy();
        }
    }
}
