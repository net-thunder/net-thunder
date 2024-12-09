package io.jaspercloud.sdwan.server.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String saveFile(MultipartFile multipartFile);

    String unzipFile(String zipFilePath, String platform, String entryFileName);

    String calcFileMd5(String path);
}
