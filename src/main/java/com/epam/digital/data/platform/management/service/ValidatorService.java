package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.model.ValidationResult;
import org.springframework.web.multipart.MultipartFile;

public interface ValidatorService {
    ValidationResult validate(MultipartFile file);
}
