package com.epam.digital.data.platform.upload.service;

import com.epam.digital.data.platform.upload.model.SecurityContext;

public interface OpenShiftService {
    void startImport(SecurityContext securityContext);
}
