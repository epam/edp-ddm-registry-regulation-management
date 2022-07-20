package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.model.SecurityContext;

public interface OpenShiftService {
    void startImport(SecurityContext securityContext);
}
