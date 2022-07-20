package com.epam.digital.data.platform.upload.service;

import com.epam.digital.data.platform.upload.model.ValidationResult;
import org.springframework.web.multipart.MultipartFile;

public interface ValidatorService {
    ValidationResult validate(MultipartFile file);
}
