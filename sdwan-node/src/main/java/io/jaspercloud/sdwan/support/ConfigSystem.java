package io.jaspercloud.sdwan.support;

import cn.hutool.setting.yaml.YamlUtil;

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
}
