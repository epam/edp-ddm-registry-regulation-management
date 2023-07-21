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
package com.epam.digital.data.platform.management.filemanagement.service;

import com.epam.digital.data.platform.management.filemanagement.mapper.FileManagementMapper;
import com.epam.digital.data.platform.management.filemanagement.model.VersionedFileInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.service.GerritService;
import com.epam.digital.data.platform.management.gitintegration.service.JGitService;
import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;
import org.springframework.lang.NonNull;

public class HeadFileRepositoryImpl extends AbstractVersionFileRepository {

  public HeadFileRepositoryImpl(String versionId, JGitService gitService,
      GerritService gerritService, FileManagementMapper mapper) {
    super(versionId, gitService, gerritService, mapper);
  }

  @Override
  @NonNull
  public List<VersionedFileInfoDto> getFileList(@NonNull String path) {
    return gitService.getFilesInPath(versionId, path)
        .stream()
        .filter(Predicate.not(DOT_GIT_KEEP::equals))
        .map(el -> {
          var filePath = FilenameUtils.normalize(Path.of(path, el).toString(), true);
          var dates = gitService.getDates(versionId, filePath);
          if (Objects.isNull(dates)) {
            // It should be impossible to get null dates of the file that exists in git repo
            throw new IllegalStateException("Got null dates on existing file in the git repo");
          }
          return mapper.toVersionedFileInfoDto(filePath, dates);
        })
        .sorted(Comparator.comparing(VersionedFileInfoDto::getName))
        .collect(Collectors.toList());
  }

  @Override
  public boolean isFileExists(@NonNull String path) {
    var file = new File(path);
    var fileName = file.getName();
    var parent = file.getParent();
    return gitService.getFilesInPath(versionId, parent).stream().anyMatch(fileName::equals);
  }

  @Override
  public void updateRepository() {
    gitService.cloneRepoIfNotExist(versionId);
  }

  @Override
  public void writeFile(@NonNull String path, @NonNull String content, String eTag) {
    gitService.commitAndSubmit(versionId, path, content, eTag);
  }

  @Override
  public void writeFile(@NonNull String path, @NonNull String content) {
    gitService.commitAndSubmit(versionId, path, content, null);
  }

  @Override
  public String readFile(@NonNull String path) {
    gitService.resetHeadBranchToRemote();
    return super.readFile(path);
  }
}
