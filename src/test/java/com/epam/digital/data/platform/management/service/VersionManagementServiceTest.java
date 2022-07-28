package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.service.impl.VersionManagementServiceImpl;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.common.ChangeInfo;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class VersionManagementServiceTest {

  @Mock
  private GerritService gerritService;

  @Mock
  private JGitService jGitService;

  @Mock
  private GerritPropertiesConfig config;

  @Mock
  private VersionedFileRepositoryFactory factory;

  @InjectMocks
  private VersionManagementServiceImpl managementService;

  @Test
  @SneakyThrows
  void getVersionsListTest() {
    List<ChangeInfo> list = new ArrayList<>();
    Mockito.when(gerritService.getMRList()).thenReturn(list);
    List<com.epam.digital.data.platform.management.model.dto.ChangeInfo> versionsList = managementService.getVersionsList();
    Assertions.assertNotNull(versionsList);
  }

  @Test
  @SneakyThrows
  void getDetailsOfHeadMasterTest() {
    List<String> list = new ArrayList<>();
    list.add("details");
    Mockito.when(jGitService.getFilesInPath(any(), any())).thenReturn(list);
    List<String> detailsOfHeadMaster = managementService.getDetailsOfHeadMaster("path");
    Assertions.assertNotNull(detailsOfHeadMaster);
    Assertions.assertEquals("details", detailsOfHeadMaster.get(0));
  }

  @Test
  @SneakyThrows
  void createNewVersionTest() {
    Mockito.when(gerritService.createChanges(any())).thenReturn("changeId");
    String version = managementService.createNewVersion("version");
    Assertions.assertEquals("changeId", version);
  }

  @Test
  @SneakyThrows
  void getVersionDetailsTest() {
    List<ChangeInfo> list = new ArrayList<>();
    ChangeInfo changeInfo = new ChangeInfo();
    changeInfo._number = 1;
    changeInfo.owner = new AccountInfo(1);
    changeInfo.labels = new HashMap<>();
    list.add(changeInfo);

    Mockito.when(gerritService.getMRList()).thenReturn(list);
    com.epam.digital.data.platform.management.model.dto.ChangeInfo version = managementService.getVersionDetails("1");
    Assertions.assertNotNull(version);
  }

}
