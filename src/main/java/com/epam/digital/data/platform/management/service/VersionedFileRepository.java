package com.epam.digital.data.platform.management.service;

import com.google.gerrit.extensions.restapi.RestApiException;

public interface VersionedFileRepository {
    void writeFile(String path, String content) throws Exception;
    String readFile(String path) throws Exception;
    boolean isFileExists(String path) throws Exception;
    String deleteFile(String path) throws Exception;

    String getVersionId();
    void pullRepository() throws Exception;
}
