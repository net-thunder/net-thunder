package io.jaspercloud.sdwan.tun.windows;

import io.jaspercloud.sdwan.tun.CheckInvoke;
import io.jaspercloud.sdwan.tun.ProcessUtil;
import io.jaspercloud.sdwan.util.SystemFile;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.InetAddress;
import java.util.List;

@Slf4j
public final class Ics {

    public static final String IcsIp = "192.168.137.1";

    public static void enable(String publicIp, String privateIp, boolean status) throws Exception {
        File vbs = SystemFile.getFile("ics.vbs");
        SystemFile.writeClassFile("script/ics.vbs", vbs);
        String hostName = InetAddress.getByName(publicIp).getHostName();
        String publicGuid = getGuid(hostName, publicIp);
        String privateGuid = getGuid(hostName, privateIp);
        File wscript = new File("C:/Windows/System32/wscript.exe");
        String cmd = String.format("%s %s \"%s\" \"%s\" %s", wscript.getAbsoluteFile(), vbs.getAbsoluteFile(), publicGuid, privateGuid, status);
        log.debug("cmd: {}", cmd);
        int code = ProcessUtil.exec(cmd);
        CheckInvoke.check(code, 0);
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
