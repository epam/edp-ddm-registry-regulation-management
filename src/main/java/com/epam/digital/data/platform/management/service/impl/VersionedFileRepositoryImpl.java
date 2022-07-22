package com.epam.digital.data.platform.management.service.impl;

import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.epam.digital.data.platform.management.model.dto.FormResponse;
import com.epam.digital.data.platform.management.model.dto.FormStatus;
import com.epam.digital.data.platform.management.model.dto.VersioningRequestDto;
import com.epam.digital.data.platform.management.service.GerritService;
import com.epam.digital.data.platform.management.service.JGitService;
import com.epam.digital.data.platform.management.service.VersionedFileRepository;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.FileInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This repo is for branches except master branch
 */
@Setter
public class VersionedFileRepositoryImpl implements VersionedFileRepository {

    private String versionName;

    private JGitService jGitService;

    private GerritService gerritService;
    private static final String FILE_DOES_NOT_EXIST = "File does not exist";

    @Override
    public List<FormResponse> getFileList() throws Exception {
        return getFileList(File.pathSeparator);
    }

    @Override
    public List<FormResponse> getFileList(String path) throws Exception {
        Map<String, FormResponse> formsInMaster = jGitService.getFilesInPath(versionName, path).stream()
                .map(el -> FormResponse.builder().name(el).status(FormStatus.CURRENT).build())
                .collect(Collectors.toMap(FormResponse::getName, Function.identity()));

        gerritService.getListOfChangesInMR(getChangeId()).forEach((key, value) -> {
            if (key.contains(path)) {
                FormResponse formsResponseDto = formsInMaster.get(FilenameUtils.getName(key));
                if (formsResponseDto == null) {
                    formsInMaster.put(key, FormResponse.builder().name(key).status(FormStatus.NEW).build());
                } else {
                    formsResponseDto.setStatus(getStatus(value));
                }
            }
        });
        return new ArrayList<>(formsInMaster.values());
    }

    @Override
    public void writeFile(String path, String content) throws Exception {
        String changeId = getChangeId();
        if (changeId != null) {
            ChangeInfoDto changeInfo = gerritService.getChangeInfo(changeId);
            VersioningRequestDto dto = VersioningRequestDto.builder()
                    .formName(path)
                    .versionName(versionName)
                    .content(content).build();
            jGitService.amend(dto, changeInfo);
        }
    }

    @Override
    public String readFile(String path) throws RestApiException {
        return gerritService.getFileContent(getChangeId(), path);
    }

    @Override
    public boolean isFileExists(String path) throws Exception {
// TODO check
        //        String changeId = getChangeId();
//        if (changeId != null) {
//            return gerritService.getListOfChangesInMR(changeId).entrySet().stream()
//                    .anyMatch(e -> e.getKey().equals(path));
//        }
//        return false;
        File theFile = new File(path);
        String parent = theFile.getParent();
        return listFilesInHead(parent).stream().anyMatch(f -> theFile.getName().equals(f));
    }

    @Override
    public String deleteFile(String path) throws Exception {
        File theFile = new File(path);
        String parent = theFile.getParent();
        if (isFileExists(path)) {
            return FILE_DOES_NOT_EXIST;
        }

        String changeId = getChangeId();
        if(changeId != null) {
            ChangeInfoDto changeInfo = gerritService.getChangeInfo(changeId);
            return jGitService.delete(changeInfo, path);
        }
        return FILE_DOES_NOT_EXIST;
    }

    @Override
    public String getVersionId() {
        return versionName;
    }

    @Override
    public void pullRepository() throws Exception {
        jGitService.cloneRepo(versionName);
    }

    private List<String> listFilesInHead(String path) throws Exception {
        return jGitService.getFilesInPath(versionName, path);
    }

    private String getChangeId() throws RestApiException {
        List<ChangeInfo> mrList = gerritService.getMRList();
        ChangeInfo changeInfo = mrList.stream()
                .filter(change -> versionName != null && versionName.equals(String.valueOf(change._number)))
                .findFirst()
                .orElse(null);
        return changeInfo != null ? changeInfo.changeId : null;
    }

    private FormStatus getStatus(FileInfo fileInfo){
        Character status = fileInfo.status;
        if(status == null) {
            return FormStatus.CHANGED;
        }
        if(status.toString().equals("A")) {
            return FormStatus.NEW;
        }
        if(status.toString().equals("D")) {
            return FormStatus.DELETED;
        }
        return null;
    }
}
