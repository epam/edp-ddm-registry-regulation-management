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

package com.epam.digital.data.platform.management.versionmanagement.service;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.core.event.publisher.RegistryRegulationManagementEventPublisher;
import com.epam.digital.data.platform.management.core.service.CacheService;
import com.epam.digital.data.platform.management.filemanagement.model.FileStatus;
import com.epam.digital.data.platform.management.forms.service.FormService;
import com.epam.digital.data.platform.management.gerritintegration.exception.GerritChangeNotFoundException;
import com.epam.digital.data.platform.management.gerritintegration.model.CreateChangeInputDto;
import com.epam.digital.data.platform.management.gerritintegration.service.GerritService;
import com.epam.digital.data.platform.management.gitintegration.service.JGitService;
import com.epam.digital.data.platform.management.groups.service.GroupService;
import com.epam.digital.data.platform.management.model.dto.DataModelFileStatus;
import com.epam.digital.data.platform.management.service.BusinessProcessService;
import com.epam.digital.data.platform.management.service.DataModelFileManagementService;
import com.epam.digital.data.platform.management.versionmanagement.mapper.VersionManagementMapper;
import com.epam.digital.data.platform.management.versionmanagement.model.DataModelChangesInfoDto;
import com.epam.digital.data.platform.management.versionmanagement.model.EntityChangesInfoDto;
import com.epam.digital.data.platform.management.versionmanagement.model.VersionChangesDto;
import com.epam.digital.data.platform.management.versionmanagement.model.VersionInfoDto;
import com.epam.digital.data.platform.management.versionmanagement.model.VersionInfoShortDto;
import com.epam.digital.data.platform.management.versionmanagement.model.VersionedFileInfoDto;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VersionManagementServiceImpl implements VersionManagementService {

  private final GerritService gerritService;
  private final JGitService jGitService;

  private final FormService formService;
  private final BusinessProcessService businessProcessService;
  private final DataModelFileManagementService dataModelFileManagementService;
  private final GroupService groupService;
  private final CacheService cacheService;
  private final GerritPropertiesConfig gerritPropertiesConfig;

  private final RegistryRegulationManagementEventPublisher eventPublisher;

  private final VersionManagementMapper versionManagementMapper;

  @Override
  public List<VersionInfoShortDto> getVersionsList() {
    return gerritService.getMRList().stream()
        .map(versionManagementMapper::toVersionInfoDto)
        .collect(Collectors.toList());
  }

  @Override
  @Nullable
  public VersionInfoDto getMasterInfo() {
    var changeInfo = gerritService.getLastMergedMR();
    return versionManagementMapper.toVersionInfoDto(changeInfo, null);
  }

  @Override
  public void decline(String versionName) {
    gerritService.declineChange(versionName);
  }

  @Override
  public boolean markReviewed(String versionName) {
    return gerritService.review(versionName);
  }

  @Override
  public void submit(String versionName) {
    gerritService.submitChanges(versionName);
    cacheService.clearCatalogCache(gerritPropertiesConfig.getHeadBranch());
  }

  @Override
  public void rebase(String versionName) {
    log.debug("Rebasing {} version candidate", versionName);
    var mr = gerritService.getMRByNumber(versionName);
    gerritService.rebase(mr.getChangeId());
    cacheService.clearCatalogCache(versionName);

    log.debug("Fetching {} version candidate on remote ref", versionName);
    var changeInfoDto = gerritService.getChangeInfo(mr.getChangeId());
    jGitService.fetch(versionName, changeInfoDto.getRefs());

    if (!changeInfoDto.getMergeable()) {
      cacheService.updateConflictsCache(versionName, jGitService.getConflicts(versionName));
    } else {
      cacheService.updateConflictsCache(versionName, null);
    }

    cacheService.updateLatestRebaseCache(versionName, LocalDateTime.now());
  }

  @Override
  public List<VersionedFileInfoDto> getVersionFileList(String versionName) {
    return gerritService.getListOfChangesInMR(versionName).entrySet().stream()
        .map(file -> versionManagementMapper.toVersionedFileInfoDto(file.getKey(), file.getValue()))
        .collect(Collectors.toList());
  }

  @Override
  public String createNewVersion(CreateChangeInputDto createChangeInputDto) {
    var versionNumber = gerritService.createChanges(createChangeInputDto);
    eventPublisher.publishVersionCandidateCreatedEvent(versionNumber);
    return versionNumber;
  }

  @Override
  public VersionInfoDto getVersionDetails(String versionName) {
    var e = gerritService.getMRByNumber(versionName);
    if (Objects.isNull(e)) {
      throw new GerritChangeNotFoundException("Could not find candidate with id " + versionName);
    }

    return versionManagementMapper.toVersionInfoDto(
        e, cacheService.getLatestRebaseCache(versionName));
  }

  @Override
  public VersionChangesDto getVersionChanges(String versionCandidateId) {
    log.debug("Selecting form changes for version candidate {}", versionCandidateId);
    var forms = getFormsChanges(versionCandidateId);

    log.debug("Selecting business-process changes for version candidate {}", versionCandidateId);
    var businessProcesses = getBusinessProcessesChanges(versionCandidateId);

    log.debug("Selecting data-model changes for version candidate {}", versionCandidateId);
    var dataModelChanges = getDataModelChanges(versionCandidateId);

    log.debug(
        "Changed: {} forms and {} business-processes", forms.size(), businessProcesses.size());
    var groups =
        versionManagementMapper.groupingToChangeInfo(
            groupService.getChangesByVersion(versionCandidateId));

    return VersionChangesDto.builder()
        .changedBusinessProcesses(businessProcesses)
        .changedForms(forms)
        .changedDataModelFiles(dataModelChanges)
        .changedGroups(groups == null ? new ArrayList<>() : List.of(groups))
        .build();
  }

  private List<EntityChangesInfoDto> getFormsChanges(String versionCandidateId) {
    return formService.getChangedFormsListByVersion(versionCandidateId).stream()
        .filter(dto -> !FileStatus.UNCHANGED.equals(dto.getStatus()))
        .map(versionManagementMapper::formInfoDtoToChangeInfo)
        .collect(Collectors.toList());
  }

  private List<EntityChangesInfoDto> getBusinessProcessesChanges(String versionCandidateId) {
    return businessProcessService.getChangedProcessesByVersion(versionCandidateId).stream()
        .filter(dto -> !FileStatus.UNCHANGED.equals(dto.getStatus()))
        .map(versionManagementMapper::bpInfoDtoToChangeInfo)
        .collect(Collectors.toList());
  }

  private List<DataModelChangesInfoDto> getDataModelChanges(String versionCandidateId) {
    return dataModelFileManagementService.listDataModelFiles(versionCandidateId).stream()
        .filter(dto -> !DataModelFileStatus.UNCHANGED.equals(dto.getStatus()))
        .map(versionManagementMapper::toDataModelChangesInfoDto)
        .collect(Collectors.toList());
  }
}
