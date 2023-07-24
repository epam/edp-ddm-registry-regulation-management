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
import com.epam.digital.data.platform.management.filemanagement.model.FileStatus;
import com.epam.digital.data.platform.management.filemanagement.model.VersionedFileInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.model.ChangeInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.model.FileInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.service.GerritService;
import com.epam.digital.data.platform.management.gitintegration.exception.RepositoryNotFoundException;
import com.epam.digital.data.platform.management.gitintegration.service.JGitService;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;
import org.springframework.lang.NonNull;


public class VersionedFileRepositoryImpl extends AbstractVersionFileRepository {

  public VersionedFileRepositoryImpl(String versionId, JGitService gitService,
      GerritService gerritService, FileManagementMapper mapper) {
    super(versionId, gitService, gerritService, mapper);
  }

  @Override
  @NonNull
  public List<VersionedFileInfoDto> getFileList(@NonNull String path) {
    Map<String, VersionedFileInfoDto> filesInMaster = gitService.getFilesInPath(versionId, path)
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
        .collect(Collectors.toMap(VersionedFileInfoDto::getName, Function.identity()));

    gerritService.getListOfChangesInMR(getChangeId()).forEach((key, value) -> {
      if (key.startsWith(path)) {
        VersionedFileInfoDto filesResponseDto = searchFileInMap(filesInMaster, key);
        if (filesResponseDto == null) {
          filesInMaster.put(FilenameUtils.getBaseName(key), VersionedFileInfoDto.builder()
              .name(FilenameUtils.getBaseName(key))
              .status(getStatus(value))
              .build());
        } else {
          filesResponseDto.setStatus(getStatus(value));
        }
      }
    });
    var forms = new ArrayList<>(filesInMaster.values());
    forms.sort(Comparator.comparing(VersionedFileInfoDto::getName));
    return forms;
  }

  private VersionedFileInfoDto searchFileInMap(Map<String, VersionedFileInfoDto> map,
      String fileKey) {
    return map.get(FilenameUtils.getBaseName(fileKey));
  }

  @Override
  public void writeFile(@NonNull String path, @NonNull String content) {
    gitService.amend(versionId, path, content, null);
  }

  @Override
  public void writeFile(@NonNull String path, @NonNull String content, String eTag) {
    gitService.amend(versionId, path, content, eTag);
  }

  @Override
  public boolean isFileExists(@NonNull String path) {
    File theFile = new File(path);
    String parent = theFile.getParent();
    String baseName = FilenameUtils.getBaseName(theFile.getName());
    return getFileList(parent).stream()
        .filter(fileResponse -> !FileStatus.DELETED.equals(fileResponse.getStatus()))
        .anyMatch(f -> baseName.equals(f.getName()));
  }

  @Override
  public void deleteFile(@NonNull String path, String eTag) {
    gitService.delete(versionId, path, eTag);
  }

  @Override
  public void updateRepository() {
    var changeId = getChangeId();
    if (changeId == null) {
      throw new RepositoryNotFoundException("Version " + versionId + " not found", versionId);
    } else {
      gitService.cloneRepoIfNotExist(versionId);
      var changeInfo = gerritService.getChangeInfo(changeId);
      gitService.fetch(versionId, changeInfo.getRefs());
    }
  }

  @Override
  public void rollbackFile(@NonNull String filePath) {
    gitService.rollbackFile(versionId, filePath);
  }

  private String getChangeId() {
    ChangeInfoDto changeInfo = gerritService.getMRByNumber(versionId);
    return changeInfo != null ? changeInfo.getChangeId() : null;
  }

  private FileStatus getStatus(FileInfoDto fileInfo) {
    String status = fileInfo.getStatus();
    if (Objects.isNull(status) || status.equals("R")) {
      return FileStatus.CHANGED;
    }
    if (status.equals("A") || status.equals("C")) {
      return FileStatus.NEW;
    }
    if (status.equals("D")) {
      return FileStatus.DELETED;
    }
    return null;
  }
}
