package io.jaspercloud.sdwan.server.service;

import io.jaspercloud.sdwan.server.controller.request.EditAppVersionRequest;
import io.jaspercloud.sdwan.server.entity.AppVersion;

import java.util.List;

public interface AppVersionService {

    void add(EditAppVersionRequest request);

    void del(EditAppVersionRequest request);

    List<AppVersion> list();

    List<AppVersion> lastVersionList();

    AppVersion queryLastVersion(String platform);
}
