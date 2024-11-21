package io.jaspercloud.sdwan.util;

import cn.hutool.core.io.file.FileNameUtil;
import io.jaspercloud.sdwan.node.support.PathApi;

import java.io.*;

public class AppFile {

    public static File getLauncherJar() throws IOException {
        File file = new File(PathApi.getAppDir(), "net-thunder.cfg");
        String line;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"))) {
            while (null != (line = reader.readLine())) {
                if (line.startsWith("app.classpath=")) {
                    String name = FileNameUtil.getName(line.replaceAll("app.classpath=", ""));
                    File jar = new File(PathApi.getAppDir(), name);
                    return jar;
                }
            }
        }
        throw new UnsupportedEncodingException();
    }
}
