package io.jaspercloud.sdwan.server.controller;

import io.jaspercloud.sdwan.server.service.StorageService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/file")
public class UploadFileController {

    @Resource
    private StorageService storageService;

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) {
        String path = storageService.saveFile(file);
        return path;
    }
}
