package io.jaspercloud.sdwan.util;

import java.io.*;

public final class SystemFile {

    public static final String System = "C:/Windows/System32";

    public static File getFile(String fileName) {
        return new File(System, fileName);
    }

    public static void writeClassFile(String src, String target) throws IOException {
        writeClassFile(src, new File(System, target));
    }

    public static void writeClassFile(String src, File target) throws IOException {
        try (InputStream in = SystemFile.class.getClassLoader().getResourceAsStream(src)) {
            try (OutputStream out = new FileOutputStream(target)) {
                byte[] bytes = new byte[1024];
                int ret;
                while (-1 != (ret = in.read(bytes))) {
                    out.write(bytes, 0, ret);
                }
                out.flush();
            }
        }
    }
}
