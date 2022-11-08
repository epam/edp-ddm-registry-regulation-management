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
package com.epam.digital.data.platform.management.service.impl;

import com.epam.digital.data.platform.management.gerritintegration.model.ChangeInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.model.FileInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.service.GerritService;
import com.epam.digital.data.platform.management.gitintegration.exception.RepositoryNotFoundException;
import com.epam.digital.data.platform.management.gitintegration.model.FileDatesDto;
import com.epam.digital.data.platform.management.gitintegration.service.JGitService;
import com.epam.digital.data.platform.management.model.dto.FileResponse;
import com.epam.digital.data.platform.management.model.dto.FileStatus;
import com.epam.digital.data.platform.management.model.dto.VersioningRequestDto;
import com.epam.digital.data.platform.management.service.VersionedFileRepository;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * This repo is for branches except master branch
 */
@Setter
public class VersionedFileRepositoryImpl implements VersionedFileRepository {

  private String versionName;

  private JGitService jGitService;

  private GerritService gerritService;

  @Override
  public List<FileResponse> getFileList() {
    return getFileList(File.separator);
  }

  @Override
  public List<FileResponse> getFileList(String path) {
    Map<String, FileResponse> filesInMaster = jGitService.getFilesInPath(versionName, path)
        .stream()
        .filter(el -> !el.equals(".gitkeep"))
        .map(el -> {
          FileDatesDto dates = jGitService.getDates(versionName, path + "/" + el);
          return FileResponse.builder()
              .name(FilenameUtils.getBaseName(el))
              .status(FileStatus.CURRENT)
              .created(dates.getCreate())
              .updated(dates.getUpdate())
              .path(path)
              .build();
        })
        .collect(
            Collectors.toMap(fileResponse -> FilenameUtils.getBaseName(fileResponse.getName()),
                Function.identity()));

    gerritService.getListOfChangesInMR(getChangeId()).forEach((key, value) -> {
      if (key.startsWith(path)) {
        FileResponse filesResponseDto = searchFileInMap(filesInMaster, key);
        if (filesResponseDto == null) {
          filesInMaster.put(FilenameUtils.getBaseName(key), FileResponse.builder()
              .name(FilenameUtils.getBaseName(key))
              .status(getStatus(value))
              .build());
        } else {
          filesResponseDto.setStatus(getStatus(value));
        }
      }
    });
    var forms = new ArrayList<>(filesInMaster.values());
    forms.sort(Comparator.comparing(FileResponse::getName));
    return forms;
  }

  private FileResponse searchFileInMap(Map<String, FileResponse> map, String fileKey) {
    return map.get(FilenameUtils.getBaseName(fileKey));
  }

  @Override
  public void writeFile(String path, String content)
      throws GitAPIException, URISyntaxException, IOException {
    String changeId = getChangeId();
    if (changeId != null) {
      ChangeInfoDto changeInfo = gerritService.getChangeInfo(changeId);
      VersioningRequestDto dto = VersioningRequestDto.builder()
          .formName(path)
          .versionName(versionName)
          .content(content).build();
      jGitService.amend(dto.getVersionName(), changeInfo.getRefs(), changeInfo.getSubject(),
          changeInfo.getChangeId(), dto.getFormName(), dto.getContent());
    }
  }

  @Override
  public String readFile(String path) throws IOException {
    return jGitService.getFileContent(versionName,
        URLDecoder.decode(path, Charset.defaultCharset()));
  }

  @Override
  public boolean isFileExists(String path) throws IOException {
    File theFile = new File(path);
    String parent = theFile.getParent();
    String baseName = FilenameUtils.getBaseName(theFile.getName());
    return getFileList(parent).stream()
        .filter(fileResponse -> !FileStatus.DELETED.equals(fileResponse.getStatus()))
        .anyMatch(f -> baseName.equals(f.getName()));
  }

  @Override
  public void deleteFile(String path) throws GitAPIException, URISyntaxException {
    String changeId = getChangeId();
    if (changeId != null) {
      ChangeInfoDto changeInfo = gerritService.getChangeInfo(changeId);
      jGitService.delete(changeInfo.getNumber(), path, changeInfo.getRefs(),
          changeInfo.getSubject(), changeInfo.getChangeId());
    }
  }

  @Override
  public String getVersionId() {
    return versionName;
  }

  @Override
  public void pullRepository() {
    var changeId = getChangeId();
    if (changeId == null) {
      throw new RepositoryNotFoundException("Version " + versionName + " not found", versionName);
    } else {
      jGitService.cloneRepoIfNotExist(versionName);
      var changeInfo = gerritService.getChangeInfo(changeId);
      jGitService.fetch(versionName, changeInfo.getRefs());
    }
  }

  private String getChangeId() {
    ChangeInfoDto changeInfo = gerritService.getMRByNumber(versionName);
    return changeInfo != null ? changeInfo.getChangeId() : null;
  }

  private ChangeInfoDto getChangeInfo() {
    return gerritService.getMRByNumber(versionName);
  }

  private FileStatus getStatus(FileInfoDto fileInfo) {
    String status = fileInfo.getStatus();
    if (Objects.isNull(status) || status.equals("R")) {
      return FileStatus.CHANGED;
    }
    if (status.equals("A")) {
      return FileStatus.NEW;
    }
    if (status.equals("D")) {
      return FileStatus.DELETED;
    }
    return null;
  }
}
