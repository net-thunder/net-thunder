package io.jaspercloud.sdwan.support;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public final class OsxShell {

    private OsxShell() {

    }

    public static void executeScriptRoot(String path, String[] args) throws Exception {
        String format = "osascript -e 'do shell script \"%s %s\" with administrator privileges' &";
        String cmd = String.format(format, path, StringUtils.join(args, " "));
        execute(cmd, new String[0]);
    }

    public static void executeScript(String path, String[] args) throws Exception {
        String format = "osascript -e 'do shell script \"%s %s\"' &";
        String cmd = String.format(format, path, StringUtils.join(args, " "));
        execute(cmd, new String[0]);
    }

    public static void execute(String path, String[] args) throws Exception {
        List<String> list = new ArrayList<>(Arrays.asList("bash", "-c"));
        list.add(path);
        list.addAll(Arrays.asList(args));
        new ProcessBuilder(list).start();
    }
}
