package com.epam.digital.data.platform.management.service;

import java.util.Map;

public interface VersionedFileRepositoryFactory {

    VersionedFileRepository getRepoByVersion(String versionName) throws Exception;

    Map<String, VersionedFileRepository> getAvailableRepos();
}
