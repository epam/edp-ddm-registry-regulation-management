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

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.core.config.JacksonConfig;
import com.epam.digital.data.platform.management.core.context.VersionContextComponentManager;
import com.epam.digital.data.platform.management.core.utils.StringsComparisonUtils;
import com.epam.digital.data.platform.management.core.service.CacheService;
import com.epam.digital.data.platform.management.filemanagement.model.FileStatus;
import com.epam.digital.data.platform.management.filemanagement.model.VersionedFileInfoDto;
import com.epam.digital.data.platform.management.filemanagement.service.VersionedFileRepository;
import com.epam.digital.data.platform.management.forms.FormMapper;
import com.epam.digital.data.platform.management.forms.exception.FormAlreadyExistsException;
import com.epam.digital.data.platform.management.forms.exception.FormNotFoundException;
import com.epam.digital.data.platform.management.forms.model.FormInfoDto;
import com.epam.digital.data.platform.management.gitintegration.exception.FileAlreadyExistsException;
import com.epam.digital.data.platform.management.gitintegration.model.FileDatesDto;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FormServiceImpl implements FormService {

  private static final String DIRECTORY_PATH = "forms";
  private static final String JSON_FILE_EXTENSION = "json";
  public static final String FORM_CREATED_FIELD = "created";
  public static final String FORM_MODIFIED_FIELD = "modified";
  private final VersionContextComponentManager versionContextComponentManager;
  private final GerritPropertiesConfig gerritPropertiesConfig;
  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private final CacheService cacheService;

  private final FormMapper formMapper;

  @Override
  public List<FormInfoDto> getFormListByVersion(String versionName) {
    return getFormListByVersion(versionName, FileStatus.DELETED);
  }

  @Override
  public List<FormInfoDto> getChangedFormsListByVersion(String versionName) {
    return getFormListByVersion(versionName, FileStatus.UNCHANGED);
  }

  @Override
  public void createForm(String formName, String content, String versionName) {
    var time = LocalDateTime.now();
    var repo =
        versionContextComponentManager.getComponent(versionName, VersionedFileRepository.class);
    String formPath = getFormPath(formName);
    var formAlreadyExistsException = new  FormAlreadyExistsException(
        String.format("Form with path '%s' already exists", formPath));
    if (repo.isFileExists(formPath)) {
      throw formAlreadyExistsException;
    }

    content = addDatesToContent(content, time, time);
    try {
      repo.writeFile(formPath, content);
    } catch (FileAlreadyExistsException e) {
      throw formAlreadyExistsException;
    }
  }

  @Override
  public String getFormContent(String formName, String versionName) {
    var repo =
        versionContextComponentManager.getComponent(versionName, VersionedFileRepository.class);
    String formContent = repo.readFile(getFormPath(formName));
    if (formContent == null) {
      throw new FormNotFoundException("Form " + formName + " not found", formName);
    }
    return formContent;
  }

  @Override
  public void updateForm(String content, String formName, String versionName, String eTag) {
    String formPath = getFormPath(formName);
    LocalDateTime time = LocalDateTime.now();
    var repo =
        versionContextComponentManager.getComponent(versionName, VersionedFileRepository.class);
    FileDatesDto fileDatesDto = FileDatesDto.builder().build();
    if (repo.isFileExists(formPath)) {
      String oldContent = repo.readFile(formPath);
      //ignore update if difference only in modified date
      if (StringsComparisonUtils.compareIgnoringSubstring(
          oldContent, content,
          "\"modified\":",
          "Z\"")) {
        return;
      }
      fileDatesDto = getDatesFromContent(oldContent);
    }
    if (fileDatesDto.getCreate() == null) {
      fileDatesDto.setCreate(
          repo.getFileList(DIRECTORY_PATH).stream()
              .filter(fileResponse -> fileResponse.getName().equals(formName))
              .findFirst()
              .map(VersionedFileInfoDto::getCreated)
              .orElse(time));
    }
    content = addDatesToContent(content, fileDatesDto.getCreate(), time);
    repo.writeFile(formPath, content, eTag);
  }

  @Override
  public void deleteForm(String formName, String versionName, String eTag) {
    var repo =
        versionContextComponentManager.getComponent(versionName, VersionedFileRepository.class);
    repo.deleteFile(getFormPath(formName), eTag);
  }

  @Override
  public void rollbackForm(String formName, String versionName) {
    var repo = versionContextComponentManager.getComponent(versionName,
        VersionedFileRepository.class);
    repo.rollbackFile(getFormPath(formName));
  }

  private String getFormPath(String formName) {
    return String.format(
        "%s/%s.%s", DIRECTORY_PATH, FilenameUtils.getName(formName), JSON_FILE_EXTENSION);
  }

  private List<FormInfoDto> getFormListByVersion(String versionName, FileStatus skippedStatus) {
    var repo =
        versionContextComponentManager.getComponent(versionName, VersionedFileRepository.class);
    var masterRepo =
        versionContextComponentManager.getComponent(
            gerritPropertiesConfig.getHeadBranch(), VersionedFileRepository.class);
    List<VersionedFileInfoDto> fileList = repo.getFileList(DIRECTORY_PATH);
    List<FormInfoDto> forms = new ArrayList<>();
    List<String> conflicts = cacheService.getConflictsCache(versionName);
    for (VersionedFileInfoDto versionedFileInfoDto : fileList) {
      if (versionedFileInfoDto.getStatus().equals(skippedStatus)) {
        continue;
      }
      String formContent;
      if (versionedFileInfoDto.getStatus() == FileStatus.DELETED) {
        formContent = masterRepo.readFile(getFormPath(versionedFileInfoDto.getName()));
      } else {
        formContent = repo.readFile(getFormPath(versionedFileInfoDto.getName()));
      }
      FileDatesDto fileDatesDto = getDatesFromContent(formContent);
      forms.add(
          formMapper.toForm(
              versionedFileInfoDto,
              fileDatesDto,
              formContent,
              conflicts.contains(versionedFileInfoDto.getPath())));
    }
    return forms;
  }

  private FileDatesDto getDatesFromContent(String formContent) {
    LocalDateTime create = null;
    LocalDateTime update = null;
    var form = JsonParser.parseString(formContent).getAsJsonObject();
    if (form.has(FORM_CREATED_FIELD)) {
      create = parseDate(form.get(FORM_CREATED_FIELD));
    }
    if (form.has(FORM_MODIFIED_FIELD)) {
      update = parseDate(form.get(FORM_MODIFIED_FIELD));
    }
    return FileDatesDto.builder().create(create).update(update).build();
  }

  private String addDatesToContent(String content, LocalDateTime created, LocalDateTime modified) {
    var formJson = JsonParser.parseString(content).getAsJsonObject();
    formJson.addProperty(FORM_CREATED_FIELD, created.format(JacksonConfig.DATE_TIME_FORMATTER));
    formJson.addProperty(FORM_MODIFIED_FIELD, modified.format(JacksonConfig.DATE_TIME_FORMATTER));
    return gson.toJson(formJson);
  }

  private LocalDateTime parseDate(JsonElement dateElement) {
    return dateElement.isJsonNull()
        ? null
        : LocalDateTime.parse(dateElement.getAsString(), JacksonConfig.DATE_TIME_FORMATTER);
  }
}
