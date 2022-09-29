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

package com.epam.digital.data.platform.management.service.impl;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.config.JacksonConfig;
import com.epam.digital.data.platform.management.exception.FormAlreadyExistsException;
import com.epam.digital.data.platform.management.model.dto.FileDatesDto;
import com.epam.digital.data.platform.management.model.dto.FileResponse;
import com.epam.digital.data.platform.management.model.dto.FileStatus;
import com.epam.digital.data.platform.management.model.dto.FormResponse;
import com.epam.digital.data.platform.management.service.FormService;
import com.epam.digital.data.platform.management.service.VersionedFileRepository;
import com.epam.digital.data.platform.management.service.VersionedFileRepositoryFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.JsonPath;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FormServiceImpl implements FormService {
  private static final String DIRECTORY_PATH = "forms";
  private static final String JSON_FILE_EXTENSION = "json";
  public static final String FORM_TITLE_PATH = "$.title";
  public static final String FORM_CREATED_PATH = "$.created";
  public static final String FORM_MODIFIED_PATH = "$.modified";
  private final VersionedFileRepositoryFactory repoFactory;
  private final GerritPropertiesConfig gerritPropertiesConfig;
  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  @Override
  public List<FormResponse> getFormListByVersion(String versionName) throws Exception {
    return getFormListByVersion(versionName, FileStatus.DELETED);
  }

  @Override
  public List<FormResponse> getChangedFormsListByVersion(String versionName) throws Exception {
    return getFormListByVersion(versionName, FileStatus.CURRENT);
  }

  @Override
  public void createForm(String formName, String content, String versionName) throws Exception {
    var time = LocalDateTime.now();
    VersionedFileRepository repo = repoFactory.getRepoByVersion(versionName);
    String formPath = getFormPath(formName);
    if(repo.isFileExists(formPath)) {
      throw new FormAlreadyExistsException(String.format("Form with path '%s' already exists", formPath));
    }
    content = addDatesToContent(content, time, time);
    repo.writeFile(formPath, content);
  }

  @Override
  public String getFormContent(String formName, String versionName) throws Exception {
    VersionedFileRepository repo = repoFactory.getRepoByVersion(versionName);
    return repo.readFile(getFormPath(formName));
  }

  @Override
  public void updateForm(String content, String formName, String versionName) throws Exception {
    LocalDateTime time = LocalDateTime.now();
    VersionedFileRepository repo = repoFactory.getRepoByVersion(versionName);
    String oldContent = repo.readFile(getFormPath(formName));
    FileDatesDto fileDatesDto = FileDatesDto.builder().build();
    if (oldContent != null) {
      fileDatesDto = getDatesFromContent(oldContent);
    }
    if (fileDatesDto.getCreate() == null) {
      fileDatesDto.setCreate(repo.getFileList(DIRECTORY_PATH).stream()
          .filter(fileResponse -> fileResponse.getName().equals(formName))
          .findFirst().map(FileResponse::getCreated).orElse(time));
    }
    content = addDatesToContent(content, fileDatesDto.getCreate(), time);
    repo.writeFile(getFormPath(formName), content);
  }

  @Override
  public void deleteForm(String formName, String versionName) throws Exception {
    VersionedFileRepository repo = repoFactory.getRepoByVersion(versionName);
    repo.deleteFile(getFormPath(formName));
  }

  private String getFormPath(String formName) {
    return String.format("%s/%s.%s", DIRECTORY_PATH, FilenameUtils.getName(formName),
        JSON_FILE_EXTENSION);
  }

  private String getTitleFromFormContent(String formContent) {
    return JsonPath.read(formContent, FORM_TITLE_PATH);
  }

  private List<FormResponse> getFormListByVersion(String versionName, FileStatus skippedStatus) throws Exception {
    VersionedFileRepository repo = repoFactory.getRepoByVersion(versionName);
    VersionedFileRepository masterRepo = repoFactory.getRepoByVersion(gerritPropertiesConfig.getHeadBranch());
    List<FileResponse> fileList = repo.getFileList(DIRECTORY_PATH);
    List<FormResponse> forms = new ArrayList<>();
    for (FileResponse fileResponse : fileList) {
      if (fileResponse.getStatus().equals(skippedStatus)) {
        continue;
      }
      String formContent;
      if(fileResponse.getStatus() == FileStatus.DELETED) {
        formContent = masterRepo.readFile(getFormPath(fileResponse.getName()));
      } else {
        formContent = repo.readFile(getFormPath(fileResponse.getName()));
      }
      FileDatesDto fileDatesDto = getDatesFromContent(formContent);
      forms.add(FormResponse.builder()
          .name(fileResponse.getName())
          .path(fileResponse.getPath())
          .status(fileResponse.getStatus())
          .created(Optional.ofNullable(fileDatesDto.getCreate()).orElse(fileResponse.getCreated()))
          .updated(Optional.ofNullable(fileDatesDto.getUpdate()).orElse(fileResponse.getUpdated()))
          .title(getTitleFromFormContent(formContent))
          .build());
    }
    return forms;
  }

  private FileDatesDto getDatesFromContent(String formContent) {
    FileDatesDto fileDatesDto = FileDatesDto.builder().build();
    JsonObject form = JsonParser.parseString(formContent).getAsJsonObject();
    if (form.has(FORM_CREATED_PATH)) {
      fileDatesDto.setCreate(JsonPath.parse(formContent).read(FORM_CREATED_PATH));
    }
    if (form.has(FORM_MODIFIED_PATH)) {
      fileDatesDto.setUpdate(JsonPath.parse(formContent).read(FORM_MODIFIED_PATH));
    }
    return fileDatesDto;
  }

  private String addDatesToContent(String content, LocalDateTime created, LocalDateTime modified) {
    JsonObject formJson = JsonParser.parseString(content).getAsJsonObject();
    formJson.addProperty("created", created.format(JacksonConfig.DATE_TIME_FORMATTER));
    formJson.addProperty("modified", modified.format(JacksonConfig.DATE_TIME_FORMATTER));
    return gson.toJson(formJson);
  }
}
