package com.epam.digital.data.platform.management.service;

import java.util.List;

public interface VersionedFileRepositoryFactory {

    VersionedFileRepository getRepoByVersion(String versionName);

    List<VersionedFileRepository> getAvailableRepos();
}
