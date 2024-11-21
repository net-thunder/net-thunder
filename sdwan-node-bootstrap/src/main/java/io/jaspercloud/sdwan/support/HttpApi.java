package io.jaspercloud.sdwan.support;

import cn.hutool.json.JSONObject;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.util.HttpClient;
import io.jaspercloud.sdwan.util.MessageBox;

import java.io.IOException;

public class HttpApi {

    public static JSONObject checkVersion(SdWanNodeConfig config, String platform, String md5Hex) throws IOException {
        try {
            JSONObject result = HttpClient.get(String.format("http://%s/appVersion/check?platform=%s&md5=%s",
                    config.getHttpServer(), platform, md5Hex));
            return result;
        } catch (Exception e) {
            MessageBox.showError("检查自更新失败");
            throw e;
        }
    }

    public static String getControllerServer(SdWanNodeConfig config) throws Exception {
        try {
            JSONObject jsonObject = HttpClient.get(String.format("http://%s/config/controllerConfig?tenantId=%s", config.getHttpServer(), config.getTenantId()));
            String controller = jsonObject.getByPath("data", String.class);
            return controller;
        } catch (Exception e) {
            MessageBox.showError("获取配置失败");
            throw e;
        }
    }
}
