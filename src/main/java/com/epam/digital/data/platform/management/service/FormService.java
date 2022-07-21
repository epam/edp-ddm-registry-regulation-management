package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.model.dto.FormResponse;

import java.util.List;

public interface FormService {
    /**
     * Get form list for specific version
     */
    List<FormResponse> getFormListByVersion(String versionName);

    /**
     * Create new form - create form from scratch or create from copy
     */
    void createForm(String formName, String content, String versionName);

    /**
     * Get content from existing form
     */
    String getFormContent(String formName, String versionName);

    /**
     * Update the content of existing form
     */
    void updateForm(String content, String formName, String versionName);

    /**
     * Delete form
     */
    void deleteForm(String formName, String versionName);
}
