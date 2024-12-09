package io.jaspercloud.sdwan.server.service.impl;

import io.jaspercloud.sdwan.server.controller.request.EditAppVersionRequest;
import io.jaspercloud.sdwan.server.entity.AppVersion;
import io.jaspercloud.sdwan.server.repository.AppVersionRepository;
import io.jaspercloud.sdwan.server.repository.mapper.AppVersionMapper;
import io.jaspercloud.sdwan.server.repository.po.AppVersionPO;
import io.jaspercloud.sdwan.server.service.AppVersionService;
import io.jaspercloud.sdwan.server.service.StorageService;
import io.jaspercloud.sdwan.util.PlatformUtil;
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

    @Resource
    private StorageService storageService;

    @Override
    public void add(EditAppVersionRequest request) {
        String entryFileName;
        switch (request.getPlatform()) {
            case PlatformUtil.WINDOWS: {
                entryFileName = "net-thunder/app/sdwan-node-bootstrap.jar";
                break;
            }
            case PlatformUtil.MACOS: {
                entryFileName = "net-thunder.app/Contents/app/sdwan-node-bootstrap.jar";
                break;
            }
            default:
                throw new UnsupportedOperationException();
        }
        String jarPath = storageService.unzipFile(request.getPath(), request.getPlatform(), entryFileName);
        String zipMd5 = storageService.calcFileMd5(request.getPath());
        String jarMd5 = storageService.calcFileMd5(jarPath);
        Date date = new Date();
        AppVersionPO appVersionPO = new AppVersionPO();
        appVersionPO.setName(request.getName());
        appVersionPO.setDescription(request.getDescription());
        appVersionPO.setZipPath(request.getPath());
        appVersionPO.setZipMd5(zipMd5);
        appVersionPO.setJarPath(jarPath);
        appVersionPO.setJarMd5(jarMd5);
        appVersionPO.setPlatform(request.getPlatform());
        appVersionPO.setCreateTime(date);
        appVersionPO.insert();
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

    @Override
    public AppVersion queryLastVersion(String platform) {
        AppVersion appVersion = appVersionRepository.query()
                .eq(AppVersion::getPlatform, platform)
                .orderByDesc(AppVersion::getCreateTime)
                .last("limit 1")
                .one();
        return appVersion;
    }
}
