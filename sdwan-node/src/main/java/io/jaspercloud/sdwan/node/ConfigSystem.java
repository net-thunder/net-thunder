package io.jaspercloud.sdwan.node;

import cn.hutool.core.io.FileUtil;
import cn.hutool.setting.yaml.YamlUtil;
import io.jaspercloud.sdwan.node.support.PathApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class ConfigSystem {

    public SdWanNodeConfig init(String configFile) throws Exception {
        if (null == configFile) {
            throw new IllegalArgumentException("configFile is null");
        }
        try (InputStream in = new FileInputStream(configFile)) {
            return YamlUtil.load(in, SdWanNodeConfig.class);
        }
    }

    public SdWanNodeConfig initUserDir() throws Exception {
        return initUserDir("application.yaml");
    }

    public static String getConfigText() throws Exception {
        File file = new File(PathApi.getAppDir(), "application.yaml");
        if (!file.exists()) {
            return null;
        }
        String text = FileUtil.readUtf8String(file);
        return text;
    }

    public static void saveConfig(String configText) {
        File file = new File(PathApi.getAppDir(), "application.yaml");
        FileUtil.writeUtf8String(configText, file);
    }

    public SdWanNodeConfig initUserDir(String configFile) throws Exception {
        File file = new File(PathApi.getAppDir(), configFile);
        try (InputStream in = new FileInputStream(file)) {
            return YamlUtil.load(in, SdWanNodeConfig.class);
        }
    }
}
