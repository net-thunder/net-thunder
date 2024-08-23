package io.jaspercloud.sdwan.tun.windows;

import io.jaspercloud.sdwan.tun.CheckInvoke;
import io.jaspercloud.sdwan.tun.ProcessUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.List;

@Slf4j
public final class Ics {

    public static void enable(String publicIp, String privateIp, boolean status) throws Exception {
        File wscript = new File("C:/Windows/System32/wscript.exe");
        File vbs = new File("C:/Windows/System32/ics.vbs");
        genScript(vbs);
        String hostName = InetAddress.getByName(publicIp).getHostName();
        String publicGuid = getGuid(hostName, publicIp);
        String privateGuid = getGuid(hostName, privateIp);
        String cmd = String.format("%s %s \"%s\" \"%s\" %s", wscript.getAbsoluteFile(), vbs.getAbsoluteFile(), publicGuid, privateGuid, status);
        log.debug("cmd: {}", cmd);
        int code = ProcessUtil.exec(cmd);
        CheckInvoke.check(code, 0);
    }

    private static void genScript(File file) throws Exception {
        try (InputStream in = Ics.class.getClassLoader().getResourceAsStream("script/ics.vbs")) {
            try (OutputStream out = new FileOutputStream(file)) {
                byte[] bytes = new byte[1024];
                int ret;
                while (-1 != (ret = in.read(bytes))) {
                    out.write(bytes, 0, ret);
                }
                out.flush();
            }
        }
    }

    private static String getGuid(String hostName, String ip) throws Exception {
        String cmd = "wmic path Win32_NetworkAdapterConfiguration get SettingID,IPAddress /format:csv";
        List<String> list = ProcessUtil.query(cmd);
        for (String line : list) {
            if (!line.startsWith(hostName)) {
                continue;
            }
            if (line.contains(ip)) {
                String guid = line.split(",")[2];
                return guid;
            }
        }
        return null;
    }
}
