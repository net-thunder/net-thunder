package io.jaspercloud.sdwan.server.service.impl;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.crypto.digest.DigestUtil;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.component.SdWanControllerProperties;
import io.jaspercloud.sdwan.server.service.StorageService;
import io.jaspercloud.sdwan.util.ShortUUID;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

@Service
public class StorageServiceImpl implements StorageService, InitializingBean {

    @Resource
    private SdWanControllerProperties properties;

    private File storage;

    @Override
    public void afterPropertiesSet() throws Exception {
        String storagePath = properties.getHttpServer().getStorage();
        storage = new File(storagePath);
        if (!storage.exists()) {
            storage.mkdir();
        }
    }

    @Override
    public String saveFile(MultipartFile multipartFile) {
        long size = multipartFile.getSize();
        String suffix = FileNameUtil.getSuffix(multipartFile.getOriginalFilename());
        String fileName = String.format("%s.%s", ShortUUID.gen(), suffix);
        File file = new File(storage, fileName);
        try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file))) {
            try (BufferedInputStream input = new BufferedInputStream(multipartFile.getInputStream())) {
                StreamUtils.copyRange(input, output, 0, size);
            }
            return fileName;
        } catch (Exception e) {
            throw new ProcessException("保存失败");
        }
    }

    @Override
    public String calcMd5(String path) {
        File file = new File(storage, path);
        if (!file.exists()) {
            throw new ProcessException("文件不存在");
        }
        String md5 = DigestUtil.md5Hex(file);
        return md5;
    }
}
