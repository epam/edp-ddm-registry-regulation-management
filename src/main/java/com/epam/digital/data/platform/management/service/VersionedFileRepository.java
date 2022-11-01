/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.model.dto.FileResponse;

import com.google.gerrit.extensions.restapi.RestApiException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import org.eclipse.jgit.api.errors.GitAPIException;

public interface VersionedFileRepository {

  List<FileResponse> getFileList() throws IOException, RestApiException;

  List<FileResponse> getFileList(String path) throws IOException, RestApiException;

  void writeFile(String path, String content) throws RestApiException, GitAPIException, URISyntaxException, IOException;

  String readFile(String path) throws IOException;

  boolean isFileExists(String path) throws IOException, RestApiException;

  void deleteFile(String path)
      throws IOException, RestApiException, GitAPIException, URISyntaxException;

  String getVersionId();

  void pullRepository();
}
