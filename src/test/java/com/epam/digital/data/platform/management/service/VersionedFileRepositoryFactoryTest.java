package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.service.impl.HeadFileRepositoryImpl;
import com.epam.digital.data.platform.management.service.impl.VersionedFileRepositoryFactoryImpl;
import com.epam.digital.data.platform.management.service.impl.VersionedFileRepositoryImpl;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class VersionedFileRepositoryFactoryTest {

    @Mock
    private JGitService jGitService;
    @Mock
    private GerritService gerritService;

    @Mock
    private GerritPropertiesConfig config;
    @InjectMocks
    private VersionedFileRepositoryFactoryImpl factory;

    @Test
    @SneakyThrows
    void getRepositoryVersionedTest() {
        Mockito.when(config.getHeadBranch()).thenReturn("master");
        VersionedFileRepository repo = factory.getRepoByVersion("version");
        Assertions.assertInstanceOf(VersionedFileRepositoryImpl.class, repo);
    }

    @Test
    @SneakyThrows
    void getRepositoryHeadTest() {
        Mockito.when(config.getHeadBranch()).thenReturn("master");
        VersionedFileRepository repo = factory.getRepoByVersion("master");
        Assertions.assertInstanceOf(HeadFileRepositoryImpl.class, repo);
    }

    @Test
    @SneakyThrows
    void getAvailReposTest() {
        Mockito.when(config.getHeadBranch()).thenReturn("master");
        factory.getRepoByVersion("version");
        factory.getRepoByVersion("master");
        Map<String, VersionedFileRepository> repositories = factory.getAvailableRepos();
        Assertions.assertEquals(2, repositories.entrySet().size());
    }
}
