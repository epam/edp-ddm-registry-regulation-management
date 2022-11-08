/*
 * Copyright 2022 EPAM Systems.
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

package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.model.dto.FormResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import org.eclipse.jgit.api.errors.GitAPIException;

public interface FormService {

  /**
   * Get form list for specific version
   */
  List<FormResponse> getFormListByVersion(String versionName) throws IOException;

  List<FormResponse> getChangedFormsListByVersion(String versionName) throws IOException;

  /**
   * Create new form - create form from scratch or create from copy
   */
  void createForm(String formName, String content, String versionName)
      throws IOException, GitAPIException, URISyntaxException;

  /**
   * Get content from existing form
   */
  String getFormContent(String formName, String versionName) throws IOException;

  /**
   * Update the content of existing form
   */
  void updateForm(String content, String formName, String versionName)
      throws IOException, GitAPIException, URISyntaxException;

  /**
   * Delete form
   */
  void deleteForm(String formName, String versionName)
      throws IOException, GitAPIException, URISyntaxException;
}
