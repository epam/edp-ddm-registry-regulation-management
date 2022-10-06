package com.epam.digital.data.platform.management.service;

import java.util.Map;

public interface VersionedFileRepositoryFactory {

    VersionedFileRepository getRepoByVersion(String versionName);

    Map<String, VersionedFileRepository> getAvailableRepos();
}
