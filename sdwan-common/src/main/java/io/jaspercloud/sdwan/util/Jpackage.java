package io.jaspercloud.sdwan.util;

import org.apache.commons.lang3.StringUtils;

public final class Jpackage {

    private Jpackage() {

    }

    public static String getAppPath() {
        String appPath = System.getProperty("jpackage.app-path");
        return appPath;
    }

    public static boolean isJpackage() {
        boolean isJpackage = StringUtils.isNotEmpty(getAppPath());
        return isJpackage;
    }
}
