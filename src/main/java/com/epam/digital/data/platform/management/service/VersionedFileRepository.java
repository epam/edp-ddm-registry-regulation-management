package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.model.dto.FormResponse;
import com.google.gerrit.extensions.restapi.RestApiException;

import java.util.List;

public interface VersionedFileRepository {

    List<FormResponse> getFileList() throws Exception;
    List<FormResponse> getFileList(String path) throws Exception;
    void writeFile(String path, String content) throws Exception;
    String readFile(String path) throws Exception;
    boolean isFileExists(String path) throws Exception;
    String deleteFile(String path) throws Exception;

    String getVersionId();
    void pullRepository() throws Exception;
}
