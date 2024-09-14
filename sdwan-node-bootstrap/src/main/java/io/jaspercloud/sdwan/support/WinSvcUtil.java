package io.jaspercloud.sdwan.support;

import com.sun.jna.platform.win32.Winsvc;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.util.CheckAdmin;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class WinSvcUtil {

    private static Logger logger = LoggerFactory.getLogger(WinSvcUtil.class);

    private WinSvcUtil() {

    }

    public static boolean isNative() {
        String kind = System.getProperties().getProperty("org.graalvm.nativeimage.kind");
        if ("executable".equals(kind)) {
            return true;
        } else {
            return false;
        }
    }

    public static String getJavaPath() {
        String javaHome = System.getProperty("java.home");
        String java = new File(new File(javaHome, "bin"), "java").getAbsolutePath();
        return java;
    }

    public static String getExecuteJarPath() {
        String userDir = System.getProperty("user.dir");
        String path = new File(userDir, "sdwan-node-bootstrap.jar").getAbsolutePath();
        return path;
    }

    public static String getExecuteBinPath() {
        String userDir = System.getProperty("user.dir");
        String path = new File(userDir, "sdwan-node-bootstrap.exe").getAbsolutePath();
        return path;
    }

    public static String getManageServiceArgs(String action, String serviceName, String logPath) {
        if (WinSvcUtil.isNative()) {
            List<String> argList = new ArrayList<>();
            argList.add(getExecuteBinPath());
            argList.add("-t");
            argList.add("manage");
            argList.add("-a");
            argList.add(action);
            argList.add("-n");
            argList.add(serviceName);
            argList.add("-log");
            argList.add(new File(logPath).getAbsolutePath());
            String path = StringUtils.join(argList, " ");
            logger.info("manageServiceArgs: {}", path);
            return path;
        } else {
            List<String> argList = new ArrayList<>();
            argList.add(getJavaPath());
            argList.add("-jar");
            argList.add(getExecuteJarPath());
            argList.add("-t");
            argList.add("manage");
            argList.add("-a");
            argList.add(action);
            argList.add("-n");
            argList.add(serviceName);
            argList.add("-log");
            argList.add(new File(logPath).getAbsolutePath());
            String path = StringUtils.join(argList, " ");
            logger.info("manageServiceArgs: {}", path);
            return path;
        }
    }

    public static void createService(String serviceName, String path) {
        CheckAdmin.check();
        try (WinServiceManager scm = WinServiceManager.openManager()) {
            scm.createService(serviceName, path);
        }
    }

    public static void createService(String serviceName, String path, int dwStartType) {
        CheckAdmin.check();
        try (WinServiceManager scm = WinServiceManager.openManager()) {
            scm.createService(serviceName, path, dwStartType);
        }
    }

    public static void startService(String serviceName) {
        CheckAdmin.check();
        try (WinServiceManager scm = WinServiceManager.openExecute()) {
            try (WinServiceManager.WinService winService = scm.openService(serviceName)) {
                if (null == winService) {
                    throw new ProcessException("not found service");
                }
                int status = queryServiceStatus(serviceName);
                if (Winsvc.SERVICE_RUNNING == status) {
                    return;
                }
                winService.start();
            }
        }
    }

    public static void stopService(String serviceName) {
        CheckAdmin.check();
        try (WinServiceManager scm = WinServiceManager.openExecute()) {
            try (WinServiceManager.WinService winService = scm.openService(serviceName)) {
                if (null == winService) {
                    return;
                }
                int status = queryServiceStatus(serviceName);
                if (Winsvc.SERVICE_STOPPED == status) {
                    return;
                }
                winService.stop();
            }
        }
    }

    public static void deleteService(String serviceName) {
        CheckAdmin.check();
        try (WinServiceManager scm = WinServiceManager.openManager()) {
            try (WinServiceManager.WinService winService = scm.openService(serviceName)) {
                if (null == winService) {
                    return;
                }
                winService.deleteService();
            }
        }
    }

    public static int queryServiceStatus(String serviceName) {
        CheckAdmin.check();
        try (WinServiceManager scm = WinServiceManager.openManager()) {
            try (WinServiceManager.WinService winService = scm.openService(serviceName)) {
                if (null == winService) {
                    return -1;
                }
                return winService.status();
            }
        }
    }
}
