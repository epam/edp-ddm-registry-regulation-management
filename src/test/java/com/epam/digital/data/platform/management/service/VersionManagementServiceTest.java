package com.epam.digital.data.platform.management.service;

import static org.mockito.ArgumentMatchers.any;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.service.impl.VersionManagementServiceImpl;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.common.ChangeInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VersionManagementServiceTest {

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
    ChangeInfo changeInfo = new ChangeInfo();
    changeInfo._number = 1;
    changeInfo.owner = new AccountInfo(1);
    changeInfo.labels = new HashMap<>();

    Mockito.when(gerritService.getMRByNumber("1")).thenReturn(changeInfo);
    com.epam.digital.data.platform.management.model.dto.ChangeInfo version =
        managementService.getVersionDetails("1");
    Assertions.assertNotNull(version);
  }

  @Test
  @SneakyThrows
  void getMasterInfo() {
    var changeInfo = new ChangeInfo();
    changeInfo.owner = new AccountInfo(1);
    changeInfo._number = 1;
    changeInfo.labels = new HashMap<>();

    Mockito.when(gerritService.getLastMergedMR()).thenReturn(changeInfo);
    var result = managementService.getMasterInfo();

    Assertions.assertNotNull(result);
    Assertions.assertEquals(result.getNumber(), changeInfo._number);
  }

  @Test
  @SneakyThrows
  void getMasterInfo_null() {
    Mockito.when(gerritService.getLastMergedMR()).thenReturn(null);
    var result = managementService.getMasterInfo();

    Assertions.assertNull(result);
  }
}
