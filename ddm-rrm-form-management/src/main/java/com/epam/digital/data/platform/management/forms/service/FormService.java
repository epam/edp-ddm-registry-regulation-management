/*
 * Copyright 2023 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.management.forms.service;

import com.epam.digital.data.platform.management.forms.model.FormInfoDto;
import java.util.List;

/**
 * Provide methods to work with forms
 */
public interface FormService {

  /**
   * Get form list for specific version
   *
   * @return {@link FormInfoDto} representation of form info
   */
  List<FormInfoDto> getFormListByVersion(String versionName);

  /**
   * Returns forms by version name
   *
   * @param versionName name of version candidate
   * @return {@link FormInfoDto} representation of form info
   */
  List<FormInfoDto> getChangedFormsListByVersion(String versionName);

  /**
   * Create new form - create form from scratch or create from copy
   *
   * @param formName    name of form
   * @param content     content of form
   * @param versionName name of version candidate
   */
  void createForm(String formName, String content, String versionName);

  /**
   * Get content from existing form
   *
   * @param formName    name of form
   * @param versionName name of version candidate
   * @return {@link String} form content
   */
  String getFormContent(String formName, String versionName);

  /**
   * Update the content of existing form
   *
   * @param content     content of form
   * @param formName    name of form
   * @param versionName name of version candidate
   */
  void updateForm(String content, String formName, String versionName, String eTag);

  /**
   * Rolls back a form to a specific version.
   *
   * @param formName    name of the form to be rolled back
   * @param versionName name of version candidate
   */
  void rollbackForm(String formName, String versionName);

  /**
   * Delete form with eTag validation
   *
   * @param formName    name of form
   * @param versionName name of version candidate
   */
  void deleteForm(String formName, String versionName, String eTag);
}
