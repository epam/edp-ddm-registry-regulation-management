package com.epam.digital.data.platform.management.service.impl;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.model.dto.BusinessProcessResponse;
import com.epam.digital.data.platform.management.model.dto.BusinessProcessChangesInfo;
import com.epam.digital.data.platform.management.model.dto.FormChangesInfo;
import com.epam.digital.data.platform.management.model.dto.ChangeInfoDetailedDto;
import com.epam.digital.data.platform.management.model.dto.CreateVersionRequest;
import com.epam.digital.data.platform.management.model.dto.FileStatus;
import com.epam.digital.data.platform.management.model.dto.FormResponse;
import com.epam.digital.data.platform.management.model.dto.VersionChanges;
import com.epam.digital.data.platform.management.model.dto.VersionedFileInfo;
import com.epam.digital.data.platform.management.model.dto.VoteRequestDto;
import com.epam.digital.data.platform.management.service.BusinessProcessService;
import com.epam.digital.data.platform.management.service.FormService;
import com.epam.digital.data.platform.management.service.GerritService;
import com.epam.digital.data.platform.management.service.JGitService;
import com.epam.digital.data.platform.management.service.VersionManagementService;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.LabelInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VersionManagementServiceImpl implements VersionManagementService {

  private final FormService formService;
  private final BusinessProcessService businessProcessService;
  private final GerritService gerritService;
  private final JGitService jGitService;
  private final GerritPropertiesConfig config;

  @Override
  public List<ChangeInfoDetailedDto> getVersionsList() throws RestApiException {
    return gerritService.getMRList().stream()
        .map(this::mapChangeInfo)
        .collect(Collectors.toList());
  }

  @Override
  @Nullable
  public ChangeInfoDetailedDto getMasterInfo() throws RestApiException {
    var changeInfo = gerritService.getLastMergedMR();
    return mapChangeInfo(changeInfo);
  }

  @Override
  public List<String> getDetailsOfHeadMaster(String path) throws Exception {
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
  public boolean vote(String versionName, String label, short value) throws RestApiException {
    var voteRequestDto = new VoteRequestDto();
    voteRequestDto.setLabel(label);
    voteRequestDto.setValue(value);
    return gerritService.vote(voteRequestDto, label);
  }

  @Override
  public List<VersionedFileInfo> getVersionFileList(String versionName) throws Exception {
    return gerritService.getListOfChangesInMR(versionName).entrySet().stream()
        .map(file -> VersionedFileInfo.builder()
            .name(file.getKey())
            .status(file.getValue().status == null ? null : file.getValue().status.toString())
            .lineInserted(file.getValue().linesInserted)
            .lineDeleted(file.getValue().linesDeleted)
            .size(file.getValue().size)
            .sizeDelta(file.getValue().sizeDelta)
            .build())
        .collect(Collectors.toList());
  }

  @Override
  public String createNewVersion(CreateVersionRequest subject) throws RestApiException {
    return gerritService.createChanges(subject);
  }

  @Override
  public ChangeInfoDetailedDto getVersionDetails(String versionName) throws RestApiException {
    var e = gerritService.getMRByNumber(versionName);
    if (Objects.isNull(e)) {
      throw new IllegalArgumentException();
    }
    return mapChangeInfo(e);
  }

  @Override
  public VersionChanges getVersionChanges(String versionCandidateId) throws Exception {
    List<FormResponse> formsList = formService.getChangedFormsListByVersion(versionCandidateId);
    List<BusinessProcessResponse> businessProcessesList = businessProcessService.getChangedProcessesByVersion(versionCandidateId);
    return VersionChanges.builder()
        .changedBusinessProcesses(businessProcessesList.stream()
            .filter(businessProcessResponse -> !businessProcessResponse.getStatus().equals(FileStatus.CURRENT))
            .map(this::toChangeInfo)
            .collect(Collectors.toList()))
        .changedForms(formsList.stream()
            .filter(e -> !e.getStatus().equals(FileStatus.CURRENT))
            .map(this::toChangeInfo).collect(Collectors.toList()))
        .build();
  }

  private ChangeInfoDetailedDto mapChangeInfo(ChangeInfo changeInfo) {
    if (Objects.isNull(changeInfo)) {
      return null;
    }
    return ChangeInfoDetailedDto.builder()
        .id(changeInfo.id)
        .number(changeInfo._number)
        .changeId(changeInfo.changeId)
        .branch(changeInfo.branch)
        .created(toUTCLocalDateTime(changeInfo.created))
        .subject(changeInfo.subject)
        .description(changeInfo.topic)
        .project(changeInfo.project)
        .submitted(toUTCLocalDateTime(changeInfo.submitted))
        .updated(toUTCLocalDateTime(changeInfo.updated))
        .owner(changeInfo.owner.username)
        .mergeable(changeInfo.mergeable)
        .labels(getResponseLabels(changeInfo.labels))
        .build();
  }

  private LocalDateTime toUTCLocalDateTime(Timestamp timestamp) {
    if (Objects.isNull(timestamp)) {
      return null;
    }
    return LocalDateTime.ofInstant(timestamp.toInstant(), ZoneId.of("UTC"));
  }

  private Map<String, Boolean> getResponseLabels(Map<String, LabelInfo> labels) {
    return labels.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().approved != null));
  }

  private BusinessProcessChangesInfo toChangeInfo(BusinessProcessResponse businessProcessResponse) {
    return BusinessProcessChangesInfo.builder()
        .name(businessProcessResponse.getName())
        .title(businessProcessResponse.getTitle())
        .status(businessProcessResponse.getStatus())
        .build();
  }

  private FormChangesInfo toChangeInfo(FormResponse formResponse) {
    return FormChangesInfo.builder()
        .name(formResponse.getName())
        .title(formResponse.getTitle())
        .status(formResponse.getStatus())
        .build();
  }
}
