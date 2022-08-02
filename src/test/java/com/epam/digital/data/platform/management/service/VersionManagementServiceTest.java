package com.epam.digital.data.platform.management.service;

import static org.mockito.ArgumentMatchers.any;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.model.dto.CreateVersionRequest;
import com.epam.digital.data.platform.management.model.dto.VersionedFileInfo;
import com.epam.digital.data.platform.management.service.impl.VersionManagementServiceImpl;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.FileInfo;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  void getVersionFileListTest() {
    Map<String, FileInfo> fileMap = new HashMap<>();
    var info = new FileInfo();
    info.status = 'A';
    info.size = 50;
    info.linesInserted = 1;
    info.linesDeleted = 0;
    info.sizeDelta = 50;
    info.binary = false;
    fileMap.put("file1", info);
    Mockito.when(gerritService.getListOfChangesInMR("3")).thenReturn(fileMap);

    List<VersionedFileInfo> res = managementService.getVersionFileList("3");

    Assertions.assertEquals(1, res.size());
    Assertions.assertEquals(50, res.get(0).getSize());
    Assertions.assertEquals("file1", res.get(0).getName());
  }

  @Test
  @SneakyThrows
  void createNewVersionTest() {
    var createVersionRequest = new CreateVersionRequest();
    createVersionRequest.setName("version");
    createVersionRequest.setDescription("description");
    Mockito.when(gerritService.createChanges(createVersionRequest)).thenReturn("changeId");
    String version = managementService.createNewVersion(createVersionRequest);
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
    var date = LocalDateTime.of(2022, 7, 29, 13, 7);
    changeInfo.updated = Timestamp.from(date.toInstant(ZoneOffset.UTC));

    Mockito.when(gerritService.getLastMergedMR()).thenReturn(changeInfo);
    var result = managementService.getMasterInfo();

    Assertions.assertNotNull(result);
    Assertions.assertEquals(result.getNumber(), changeInfo._number);
    Assertions.assertEquals(result.getUpdated(), date);
  }

  @Test
  @SneakyThrows
  void getMasterInfo_null() {
    Mockito.when(gerritService.getLastMergedMR()).thenReturn(null);
    var result = managementService.getMasterInfo();

    Assertions.assertNull(result);
  }
}
