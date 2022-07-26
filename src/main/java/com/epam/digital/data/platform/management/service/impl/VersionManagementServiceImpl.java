package com.epam.digital.data.platform.management.service.impl;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.model.dto.ChangeInfo;
import com.epam.digital.data.platform.management.model.dto.VersionedFileInfo;
import com.epam.digital.data.platform.management.service.GerritService;
import com.epam.digital.data.platform.management.service.JGitService;
import com.epam.digital.data.platform.management.service.VersionManagementService;
import com.epam.digital.data.platform.management.service.VersionedFileRepositoryFactory;
import com.google.gerrit.extensions.common.LabelInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VersionManagementServiceImpl implements VersionManagementService {

    @Autowired
    private GerritService gerritService;

    @Autowired
    private JGitService jGitService;

    @Autowired
    private GerritPropertiesConfig config;

    @Autowired
    private VersionedFileRepositoryFactory repositoryFactory;

    @Override
    public List<ChangeInfo> getVersionsList() throws RestApiException {
        return gerritService.getMRList().stream()
                .map(e -> ChangeInfo.builder()
                        .id(e.id)
                        .changeId(e.changeId)
                        .branch(e.branch)
                        .created(e.created)
                        .subject(e.subject)
                        .project(e.project)
                        .submitted(e.submitted)
                        .topic(e.topic)
                        .updated(e.updated)
                        .owner(e.owner.username)
                        .mergeable(e.mergeable)
                        .labels(getResponseLabels(e.labels))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getDetailsOfHeadMaster(String path) throws Exception {
        return jGitService.getFilesInPath(config.getHeadBranch(), path);
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
    public String createNewVersion(String subject) throws RestApiException {
        return gerritService.createChanges(subject);
    }

    @Override
    public ChangeInfo getVersionDetails(String versionName) throws RestApiException {
        return gerritService.getMRList().stream()
                .filter(c -> String.valueOf(c._number).equals(versionName))
                .map(e -> ChangeInfo.builder()
                        .id(e.id)
                        .changeId(e.changeId)
                        .branch(e.branch)
                        .created(e.created)
                        .subject(e.subject)
                        .project(e.project)
                        .submitted(e.submitted)
                        .topic(e.topic)
                        .updated(e.updated)
                        .owner(e.owner.username)
                        .mergeable(e.mergeable)
                        .labels(getResponseLabels(e.labels))
                        .build())
                .findAny()
                .orElseThrow(IllegalArgumentException::new);
    }

    private Map<String, Boolean> getResponseLabels(Map<String, LabelInfo> labels) {
        return labels.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().approved != null));
    }

    private String getChangeId(String versionName) throws RestApiException {
        List<com.google.gerrit.extensions.common.ChangeInfo> mrList = gerritService.getMRList();
        com.google.gerrit.extensions.common.ChangeInfo changeInfo = mrList.stream()
                .filter(change -> versionName != null && versionName.equals(String.valueOf(change._number)))
                .findFirst()
                .orElse(null);
        return changeInfo != null ? changeInfo.changeId : null;
    }
}
