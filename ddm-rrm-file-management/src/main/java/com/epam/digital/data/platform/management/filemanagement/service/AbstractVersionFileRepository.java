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

package com.epam.digital.data.platform.management.filemanagement.service;

import com.epam.digital.data.platform.management.filemanagement.mapper.FileManagementMapper;
import com.epam.digital.data.platform.management.gerritintegration.service.GerritService;
import com.epam.digital.data.platform.management.gitintegration.service.JGitService;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

@RequiredArgsConstructor
public abstract class AbstractVersionFileRepository implements VersionedFileRepository {

  protected static final String DOT_GIT_KEEP = ".gitkeep";

  protected final String versionId;
  protected final JGitService gitService;
  protected final GerritService gerritService;
  protected final FileManagementMapper mapper;

  @Override
  @NonNull
  public String getVersionId() {
    return versionId;
  }

  @Override
  @Nullable
  public String readFile(@NonNull String path) {
    return gitService.getFileContent(versionId,
        URLDecoder.decode(path, Charset.defaultCharset()));
  }

  @Override
  public void writeFile(@NonNull String path, @NonNull String content, String eTag) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeFile(@NonNull String path, @NonNull String content) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void rollbackFile(@NonNull String path) {
    throw new UnsupportedOperationException();
  }
}
