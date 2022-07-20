package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.epam.digital.data.platform.management.model.dto.VersioningRequestDto;
import org.eclipse.jgit.api.Git;

import java.io.File;
import java.util.List;

public interface JGitService {
    void pull() throws Exception;

    List<String> getFilesInPath(String path) throws Exception;

    String getFileContent(String filePath) throws Exception;

    String convertAndAmend(VersioningRequestDto requestDto, ChangeInfoDto changeInfoDto) throws Exception;

    String amend(File file, ChangeInfoDto changeInfoDto, Git git) throws Exception;

    String delete(ChangeInfoDto changeInfoDto, String fileName) throws Exception;
}
