package com.epam.digital.data.platform.management.service.impl;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.service.GerritService;
import com.epam.digital.data.platform.management.service.JGitService;
import com.epam.digital.data.platform.management.service.VersionedFileRepository;
import com.epam.digital.data.platform.management.service.VersionedFileRepositoryFactory;
import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
@AllArgsConstructor
public class VersionedFileRepositoryFactoryImpl implements VersionedFileRepositoryFactory {

    @Autowired
    private GerritPropertiesConfig config;

    @Autowired
    private JGitService jGitService;

    @Autowired
    private GerritService gerritService;

    private final Map<String, VersionedFileRepository> repositoryMap = new HashMap<>();

    @PostConstruct
    public void init() throws Exception {
        getRepoByVersion(config.getHeadBranch());
    }

    @Override
    public VersionedFileRepository getRepoByVersion(String versionName) throws Exception {
        VersionedFileRepository repo = null;
        boolean isNeedInit = false;
        synchronized (repositoryMap) {
            repo = repositoryMap.get(versionName);
            if (repo == null) {
                repo = doCreateRepo(versionName);
                isNeedInit = true;
                repositoryMap.put(versionName, repo);
            }
        }
        if (isNeedInit) {
            repo.pullRepository();
        }
        return repo;
    }

    @Override
    public Map<String, VersionedFileRepository> getAvailableRepos() {
        synchronized (repositoryMap) {
            return ImmutableMap.copyOf(repositoryMap);
        }
    }

    private VersionedFileRepository doCreateRepo(String versionName) {
        if (config.getHeadBranch().equals(versionName)) {
            HeadFileRepositoryImpl repo = new HeadFileRepositoryImpl();
            repo.setVersionName(versionName);
            repo.setJGitService(jGitService);
            return repo;
        } else {
            VersionedFileRepositoryImpl repo = new VersionedFileRepositoryImpl();
            repo.setVersionName(versionName);
            repo.setGerritService(gerritService);
            repo.setJGitService(jGitService);
            return repo;
        }
    }
}
