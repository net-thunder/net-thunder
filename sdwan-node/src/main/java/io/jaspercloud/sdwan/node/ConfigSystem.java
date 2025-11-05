package io.jaspercloud.sdwan.node;

import cn.hutool.core.bean.BeanDesc;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.PropDesc;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.setting.yaml.YamlUtil;
import io.jaspercloud.sdwan.node.support.PathApi;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Slf4j
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
        SdWanNodeConfig config;
        if (file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                config = YamlUtil.load(in, SdWanNodeConfig.class);
            }
            if (null == config) {
                config = new SdWanNodeConfig();
            }
        } else {
            config = new SdWanNodeConfig();
        }
        BeanDesc beanDesc = BeanUtil.getBeanDesc(SdWanNodeConfig.class);
        for (PropDesc propDesc : beanDesc.getProps()) {
            String propName = propDesc.getFieldName();
            String propValue = System.getenv(propName);
            if (null != propValue) {
                log.info("env: {}={}", propName, propValue);
            }
            if (null == propValue) {
                propValue = System.getProperty(propName);
                if (null != propValue) {
                    log.info("jvmProperty: {}={}", propName, propValue);
                }
            }
            if (null == propValue) {
                continue;
            }
            Object convertValue = Convert.convert(propDesc.getFieldType(), propValue);
            propDesc.setValue(config, convertValue);
        }
        return config;
    }
}
