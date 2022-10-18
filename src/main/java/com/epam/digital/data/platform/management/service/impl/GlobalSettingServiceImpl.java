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

import com.epam.digital.data.platform.management.exception.ReadingRepositoryException;
import com.epam.digital.data.platform.management.exception.SettingsProcessingException;
import com.epam.digital.data.platform.management.exception.WritingRepositoryException;
import com.epam.digital.data.platform.management.model.dto.GlobalSettingsInfo;
import com.epam.digital.data.platform.management.model.dto.GlobalVarsDto;
import com.epam.digital.data.platform.management.model.dto.SettingDto;
import com.epam.digital.data.platform.management.service.GlobalSettingService;
import com.epam.digital.data.platform.management.service.VersionedFileRepository;
import com.epam.digital.data.platform.management.service.VersionedFileRepositoryFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GlobalSettingServiceImpl implements GlobalSettingService {
  private static final String GLOBAL_SETTINGS_PATH = "global-vars/camunda-global-system-vars.yml";
  private static final String VERSION_SETTINGS_PATH = "settings/settings.yml";
  private final VersionedFileRepositoryFactory repoFactory;

  @Override
  public GlobalSettingsInfo getGlobalSettings(String versionCandidateId) {
    log.debug("Trying to get repo");
    VersionedFileRepository repo = repoFactory.getRepoByVersion(versionCandidateId);
    log.debug("Finished getting repo for {} version", versionCandidateId);
    String camundaGlobalVarsContent = getCamundaGlobalSettingsContent(repo);
    String settingsContent = getSettingsContent(repo);
    log.debug("Completed settings files reading");
    return parseSettingsFiles(camundaGlobalVarsContent, settingsContent);
  }

  @Override
  public void updateSettings(String versionCandidateId, GlobalSettingsInfo settings) {
    YAMLMapper mapper = new YAMLMapper(new YAMLFactory()).disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
    log.debug("YAMLMapper was initialized");
    log.debug("Trying to get repo");
    VersionedFileRepository repo = repoFactory.getRepoByVersion(versionCandidateId);
    log.debug("Finished getting repo for {} version", versionCandidateId);
    GlobalVarsDto globalVarsDto = new GlobalVarsDto(settings.getThemeFile(), settings.getSupportEmail());
    SettingDto settingDto = new SettingDto(settings.getTitleFull(), settings.getTitle(), settings.getBlacklistedDomains());
    writeSettingsContent(repo, mapper, settingDto);
    writeGlobalVarsContent(repo, mapper, globalVarsDto);
  }

  private static GlobalSettingsInfo parseSettingsFiles(String camundaGlobalVarsContent, String settingsContent) {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(JsonGenerator.Feature.IGNORE_UNKNOWN);
    log.debug("ObjectMapper was initialized");
    try {
      log.debug("Parsing settings files");
      SettingDto settingDto = mapper.readValue(settingsContent, SettingDto.class);
      log.debug("Parsed settings file");
      GlobalVarsDto globalVarsDto = mapper.readValue(camundaGlobalVarsContent, GlobalVarsDto.class);
      log.debug("Parsed global var file");
      return GlobalSettingsInfo.builder()
          .blacklistedDomains(settingDto.getBlacklistedDomains())
          .titleFull(settingDto.getTitleFull())
          .supportEmail(globalVarsDto.getSupportEmail())
          .themeFile(globalVarsDto.getThemeFile())
          .title(settingDto.getTitle())
          .build();
    } catch (JsonProcessingException e) {
      throw new SettingsProcessingException("Could not process settings files", e);
    }
  }

  private String getSettingsContent(VersionedFileRepository repo) {
    try {
      return repo.readFile(VERSION_SETTINGS_PATH);
    } catch (Exception e) {
      throw new ReadingRepositoryException("Could not read repo to get settings", e);
    }
  }

  private String getCamundaGlobalSettingsContent(VersionedFileRepository repo) {
    try {
      return repo.readFile(GLOBAL_SETTINGS_PATH);
    } catch (Exception e) {
      throw new ReadingRepositoryException("Could not read repo to get global settings", e);
    }
  }

  private void writeSettingsContent(VersionedFileRepository repo, ObjectMapper mapper, SettingDto settings) {
    try {
      log.debug("Writing settings to file");
      repo.writeFile(VERSION_SETTINGS_PATH, mapper.writeValueAsString(settings));
      log.debug("Finished writing settings");
    } catch (JsonProcessingException e) {
      throw new SettingsProcessingException("Could not process settings file", e);
    } catch (Exception e) {
      throw new WritingRepositoryException("Could not write settings", e);
    }
  }

  private void writeGlobalVarsContent(VersionedFileRepository repo, ObjectMapper mapper, GlobalVarsDto globalVars) {
    try {
      log.debug("Writing global vars to file");
      repo.writeFile(GLOBAL_SETTINGS_PATH, mapper.writeValueAsString(globalVars));
      log.debug("Finished writing global vars");
    } catch (JsonProcessingException e) {
      throw new SettingsProcessingException("Could not process global vars file", e);
    } catch (Exception e) {
      throw new WritingRepositoryException("Could not write global vars", e);
    }
  }
}
