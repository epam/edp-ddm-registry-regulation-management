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

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.core.event.publisher.RegistryRegulationManagementEventPublisher;
import com.epam.digital.data.platform.management.filemanagement.model.FileStatus;
import com.epam.digital.data.platform.management.forms.model.FormInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.exception.GerritChangeNotFoundException;
import com.epam.digital.data.platform.management.gerritintegration.model.ChangeInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.model.CreateChangeInputDto;
import com.epam.digital.data.platform.management.gerritintegration.service.GerritService;
import com.epam.digital.data.platform.management.gitintegration.service.JGitService;
import com.epam.digital.data.platform.management.model.dto.BusinessProcessChangesInfo;
import com.epam.digital.data.platform.management.model.dto.BusinessProcessInfoDto;
import com.epam.digital.data.platform.management.model.dto.ChangeInfoDetailedDto;
import com.epam.digital.data.platform.management.model.dto.CreateVersionRequest;
import com.epam.digital.data.platform.management.model.dto.FormChangesInfo;
import com.epam.digital.data.platform.management.model.dto.VersionChanges;
import com.epam.digital.data.platform.management.model.dto.VersionedFileInfo;
import com.epam.digital.data.platform.management.service.BusinessProcessService;
import com.epam.digital.data.platform.management.forms.service.FormService;
import com.epam.digital.data.platform.management.service.VersionManagementService;
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

  private final FormService formService;
  private final BusinessProcessService businessProcessService;
  private final GerritService gerritService;
  private final JGitService jGitService;
  private final GerritPropertiesConfig config;

  private final RegistryRegulationManagementEventPublisher eventPublisher;

  @Override
  public List<ChangeInfoDetailedDto> getVersionsList() {
    return gerritService.getMRList().stream()
        .map(this::mapChangeInfo)
        .collect(Collectors.toList());
  }

  @Override
  @Nullable
  public ChangeInfoDetailedDto getMasterInfo() {
    var changeInfo = gerritService.getLastMergedMR();
    return mapChangeInfo(changeInfo);
  }

  @Override
  public List<String> getDetailsOfHeadMaster(String path) {
    return jGitService.getFilesInPath(config.getHeadBranch(), path);
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
  }

  @Override
  public void rebase(String versionName) {
    log.debug("Rebasing {} version candidate", versionName);
    var mr = gerritService.getMRByNumber(versionName);
    gerritService.rebase(mr.getChangeId());

    log.debug("Fetching {} version candidate on remote ref", versionName);
    var changeInfoDto = gerritService.getChangeInfo(mr.getChangeId());
    jGitService.fetch(versionName, changeInfoDto.getRefs());
  }

  @Override
  public List<VersionedFileInfo> getVersionFileList(String versionName) {
    return gerritService.getListOfChangesInMR(versionName).entrySet().stream()
        .map(file -> VersionedFileInfo.builder()
            .name(file.getKey())
            .status(file.getValue().getStatus())
            .lineInserted(file.getValue().getLinesInserted())
            .lineDeleted(file.getValue().getLinesDeleted())
            .size(file.getValue().getSize())
            .sizeDelta(file.getValue().getSizeDelta())
            .build())
        .collect(Collectors.toList());
  }

  @Override
  public String createNewVersion(CreateVersionRequest subject) {
    final var changeInputDto = CreateChangeInputDto.builder()
        .name(subject.getName())
        .description(subject.getDescription())
        .build();
    var versionNumber = gerritService.createChanges(changeInputDto);
    eventPublisher.publishVersionCandidateCreatedEvent(versionNumber);
    return versionNumber;
  }

  @Override
  public ChangeInfoDetailedDto getVersionDetails(String versionName) {
    var e = gerritService.getMRByNumber(versionName);
    if (Objects.isNull(e)) {
      throw new GerritChangeNotFoundException("Could not find candidate with id " + versionName);
    }
    return mapChangeInfo(e);
  }

  @Override
  public VersionChanges getVersionChanges(String versionCandidateId) {
    log.debug("Selecting form changes for version candidate {}", versionCandidateId);
    var forms = formService.getChangedFormsListByVersion(versionCandidateId)
        .stream()
        .filter(e -> !e.getStatus().equals(FileStatus.CURRENT))
        .map(this::toChangeInfo)
        .collect(Collectors.toList());

    log.debug("Selecting business-process changes for version candidate {}", versionCandidateId);
    var businessProcesses = businessProcessService.getChangedProcessesByVersion(versionCandidateId)
        .stream()
        .filter(businessProcessResponse -> !businessProcessResponse.getStatus()
            .equals(FileStatus.CURRENT))
        .map(this::toChangeInfo)
        .collect(Collectors.toList());
    log.debug("Changed: {} forms and {} business-processes", forms.size(),
        businessProcesses.size());
    return VersionChanges.builder()
        .changedBusinessProcesses(businessProcesses)
        .changedForms(forms)
        .build();
  }

  private ChangeInfoDetailedDto mapChangeInfo(ChangeInfoDto changeInfo) {
    if (Objects.isNull(changeInfo)) {
      return null;
    }
    return ChangeInfoDetailedDto.builder()
        .id(changeInfo.getId())
        .number(Integer.parseInt(changeInfo.getNumber()))
        .changeId(changeInfo.getChangeId())
        .branch(changeInfo.getBranch())
        .created(changeInfo.getCreated())
        .subject(changeInfo.getSubject())
        .description(changeInfo.getTopic())
        .project(changeInfo.getProject())
        .submitted(changeInfo.getSubmitted())
        .updated(changeInfo.getUpdated())
        .owner(changeInfo.getOwner())
        .mergeable(changeInfo.getMergeable())
        .labels(changeInfo.getLabels())
        .build();
  }

  private BusinessProcessChangesInfo toChangeInfo(BusinessProcessInfoDto businessProcessInfoDto) {
    return BusinessProcessChangesInfo.builder()
        .name(businessProcessInfoDto.getName())
        .title(businessProcessInfoDto.getTitle())
        .status(businessProcessInfoDto.getStatus())
        .build();
  }

  private FormChangesInfo toChangeInfo(FormInfoDto formResponse) {
    return FormChangesInfo.builder()
        .name(formResponse.getName())
        .title(formResponse.getTitle())
        .status(formResponse.getStatus())
        .build();
  }
}
