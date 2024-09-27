package io.jaspercloud.sdwan.util;

import org.apache.commons.lang3.StringUtils;

public final class Jpackage {

    private Jpackage() {

    }

    public static boolean isJpackage() {
        String property = System.getProperty("jpackage.app-path");
        boolean isJpackage = StringUtils.isNotEmpty(property);
        return isJpackage;
    }
}
