package com.epam.digital.data.platform.management.service.impl;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.model.dto.ChangeInfo;
import com.epam.digital.data.platform.management.model.dto.CreateVersionRequest;
import com.epam.digital.data.platform.management.model.dto.VersionedFileInfo;
import com.epam.digital.data.platform.management.service.GerritService;
import com.epam.digital.data.platform.management.service.JGitService;
import com.epam.digital.data.platform.management.service.VersionManagementService;
import com.epam.digital.data.platform.management.service.VersionedFileRepositoryFactory;
import com.google.gerrit.extensions.common.LabelInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
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
            .map(this::mapChangeInfo)
            .collect(Collectors.toList());
    }

    @Override
    @Nullable
    public ChangeInfo getMasterInfo() throws RestApiException {
        var changeInfo = gerritService.getLastMergedMR();
        return mapChangeInfo(changeInfo);
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
    public String createNewVersion(CreateVersionRequest subject) throws RestApiException {
        return gerritService.createChanges(subject);
    }

    @Override
    public ChangeInfo getVersionDetails(String versionName) throws RestApiException {
        var e = gerritService.getMRByNumber(versionName);
        if (Objects.isNull(e)) {
            throw new IllegalArgumentException();
        }
        return mapChangeInfo(e);
    }

    private ChangeInfo mapChangeInfo(
        @Nullable com.google.gerrit.extensions.common.ChangeInfo changeInfo) {
        if (Objects.isNull(changeInfo)) {
            return null;
        }
        return ChangeInfo.builder()
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
}
