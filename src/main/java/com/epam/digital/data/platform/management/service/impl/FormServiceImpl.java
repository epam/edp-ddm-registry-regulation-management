package com.epam.digital.data.platform.management.service.impl;

import com.epam.digital.data.platform.management.model.dto.FileResponse;
import com.epam.digital.data.platform.management.service.FormService;
import com.epam.digital.data.platform.management.service.VersionedFileRepository;
import com.epam.digital.data.platform.management.service.VersionedFileRepositoryFactory;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class FormServiceImpl implements FormService {

    private static final String DIRECTORY_PATH = "forms";
    private static final String JSON_FILE_EXTENSION = "json";

    @Autowired
    private VersionedFileRepositoryFactory repoFactory;

    @Override
    public List<FileResponse> getFormListByVersion(String versionName) throws Exception {
        VersionedFileRepository repo = repoFactory.getRepoByVersion(versionName);
        return repo.getFileList(DIRECTORY_PATH).stream()
            .filter(fileResponse -> !fileResponse.getName().equals(".gitkeep"))
            .collect(Collectors.toList());
    }

    @Override
    public void createForm(String formName, String content, String versionName) throws Exception {
        VersionedFileRepository repo = repoFactory.getRepoByVersion(versionName);
        repo.writeFile(getFormPath(formName), content);
    }

    @Override
    public String getFormContent(String formName, String versionName) throws Exception {
        VersionedFileRepository repo = repoFactory.getRepoByVersion(versionName);
        return repo.readFile(getFormPath(formName));
    }

    @Override
    public void updateForm(String content, String formName, String versionName) throws Exception {
        VersionedFileRepository repo = repoFactory.getRepoByVersion(versionName);
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
}
