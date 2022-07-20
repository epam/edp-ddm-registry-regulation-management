package com.epam.digital.data.platform.management.service;

public interface VersionedFileRepository {
    void writeFile(String path, String content);
    String readFile(String path);
    boolean isFileExists(String path);
    void deleteFile(String path);
}
