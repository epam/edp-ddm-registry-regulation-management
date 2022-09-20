package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.model.dto.FormResponse;
import java.util.List;

public interface FormService {

  /**
   * Get form list for specific version
   */
  List<FormResponse> getFormListByVersion(String versionName) throws Exception;

  List<FormResponse> getChangedFormsListByVersion(String versionName) throws Exception;

  /**
   * Create new form - create form from scratch or create from copy
   */
  void createForm(String formName, String content, String versionName) throws Exception;

  /**
   * Get content from existing form
   */
  String getFormContent(String formName, String versionName) throws Exception;

  /**
   * Update the content of existing form
   */
  void updateForm(String content, String formName, String versionName) throws Exception;

  /**
   * Delete form
   */
  void deleteForm(String formName, String versionName) throws Exception;
}
