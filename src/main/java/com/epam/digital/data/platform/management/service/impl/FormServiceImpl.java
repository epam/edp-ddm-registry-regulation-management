package com.epam.digital.data.platform.management.service.impl;

import com.epam.digital.data.platform.management.model.dto.FileResponse;
import com.epam.digital.data.platform.management.service.FormService;
import com.epam.digital.data.platform.management.service.VersionedFileRepository;
import com.epam.digital.data.platform.management.service.VersionedFileRepositoryFactory;
import lombok.AllArgsConstructor;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
@AllArgsConstructor
public class FormServiceImpl implements FormService {

    private static final String DIRECTORY_PATH = "/forms";

    @Autowired
    private VersionedFileRepositoryFactory repoFactory;

    @Override
    public List<FileResponse> getFormListByVersion(String versionName) throws Exception {
        VersionedFileRepository repo = repoFactory.getRepoByVersion(versionName);
        return repo.getFileList(DIRECTORY_PATH);
    }

    @Override
    public void createForm(String formName, String content, String versionName) throws Exception {
        VersionedFileRepository repo = repoFactory.getRepoByVersion(versionName);
        repo.writeFile(DIRECTORY_PATH + File.pathSeparator + formName, content);
    }

    @Override
    public String getFormContent(String formName, String versionName) throws Exception {
        VersionedFileRepository repo = repoFactory.getRepoByVersion(versionName);
        return repo.readFile(DIRECTORY_PATH + File.pathSeparator + formName);
    }

    @Override
    public void updateForm(String content, String formName, String versionName) throws Exception {
        VersionedFileRepository repo = repoFactory.getRepoByVersion(versionName);
        repo.writeFile(DIRECTORY_PATH + File.pathSeparator + formName, content);
    }

    @Override
    public void deleteForm(String formName, String versionName) throws Exception {
        VersionedFileRepository repo = repoFactory.getRepoByVersion(versionName);
        repo.deleteFile(DIRECTORY_PATH + File.pathSeparator + FilenameUtils.getName(formName));
    }
}
