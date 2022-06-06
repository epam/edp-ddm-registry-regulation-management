package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.model.dto.FileResponse;

import java.util.List;

public interface VersionedFileRepository {

    List<FileResponse> getFileList() throws Exception;
    List<FileResponse> getFileList(String path) throws Exception;
    void writeFile(String path, String content) throws Exception;
    String readFile(String path) throws Exception;
    boolean isFileExists(String path) throws Exception;
    String deleteFile(String path) throws Exception;

    String getVersionId();
    void pullRepository() throws Exception;
}
