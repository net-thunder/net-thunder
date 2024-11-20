package io.jaspercloud.sdwan.server.service.impl;

import io.jaspercloud.sdwan.server.controller.request.EditAppVersionRequest;
import io.jaspercloud.sdwan.server.entity.AppVersion;
import io.jaspercloud.sdwan.server.repository.AppVersionRepository;
import io.jaspercloud.sdwan.server.repository.mapper.AppVersionMapper;
import io.jaspercloud.sdwan.server.repository.po.AppVersionPO;
import io.jaspercloud.sdwan.server.service.AppVersionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class AppVersionServiceImpl implements AppVersionService {

    @Resource
    private AppVersionRepository appVersionRepository;

    @Resource
    private AppVersionMapper appVersionMapper;

    @Override
    public void add(EditAppVersionRequest request) {
        Date date = new Date();
        request.getOsList().forEach(os -> {
            AppVersionPO appVersionPO = new AppVersionPO();
            appVersionPO.setName(request.getName());
            appVersionPO.setDescription(request.getDescription());
            appVersionPO.setPath(request.getPath());
            appVersionPO.setOs(os);
            appVersionPO.setCreateTime(date);
            appVersionPO.insert();
        });
    }

    @Override
    public void del(EditAppVersionRequest request) {
        appVersionRepository.deleteById(request.getId());
    }

    @Override
    public List<AppVersion> list() {
        List<AppVersion> list = appVersionRepository.query()
                .orderByDesc(AppVersion::getCreateTime)
                .list();
        return list;
    }

    @Override
    public List<AppVersion> lastVersionList() {
        List<AppVersionPO> list = appVersionMapper.lastVersionList();
        List<AppVersion> collect = list.stream()
                .map(e -> appVersionRepository.getTransformer().output(e))
                .collect(Collectors.toList());
        return collect;
    }
}
