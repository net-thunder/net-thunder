package io.jaspercloud.sdwan.node;

import cn.hutool.setting.yaml.YamlUtil;

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
        File file = new File(System.getProperty("user.dir"), "application.yaml");
        if (file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                return YamlUtil.load(in, SdWanNodeConfig.class);
            }
        }
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("application.yaml")) {
            return YamlUtil.load(in, SdWanNodeConfig.class);
        }
    }
}