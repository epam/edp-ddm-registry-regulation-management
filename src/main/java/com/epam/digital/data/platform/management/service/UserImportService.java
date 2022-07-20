package com.epam.digital.data.platform.upload.service;

import com.epam.digital.data.platform.upload.model.SecurityContext;
import com.epam.digital.data.platform.upload.model.dto.CephFileDto;
import com.epam.digital.data.platform.upload.model.dto.CephFileInfoDto;
import org.springframework.web.multipart.MultipartFile;

public interface UserImportService {
    CephFileInfoDto storeFile(MultipartFile file, SecurityContext securityContext);

    CephFileInfoDto getFileInfo(SecurityContext securityContext);

    void delete(String cephKey);

    CephFileDto downloadFile(String cephKey);
}
