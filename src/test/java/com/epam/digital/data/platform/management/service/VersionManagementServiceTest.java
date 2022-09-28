package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.model.dto.ChangeInfoDetailedDto;
import com.epam.digital.data.platform.management.model.dto.CreateVersionRequest;
import com.epam.digital.data.platform.management.model.dto.VersionedFileInfo;
import com.epam.digital.data.platform.management.service.impl.VersionManagementServiceImpl;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.FileInfo;
import com.google.gerrit.extensions.common.LabelInfo;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
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

  @InjectMocks
  private VersionManagementServiceImpl managementService;

  @Test
  @SneakyThrows
  void getVersionsListTest() {
    var changeInfo = new ChangeInfo();
    changeInfo.id = "changeInfoId";
    changeInfo._number = 1;
    changeInfo.changeId = "changeInfoChangeId";
    changeInfo.branch = "changeInfoBranch";
    changeInfo.created = Timestamp.from(
        LocalDateTime.of(2022, 8, 10, 17, 15).toInstant(ZoneOffset.ofHours(3)));
    changeInfo.subject = "changeInfoSubject";
    changeInfo.topic = "changeInfoTopic";
    changeInfo.project = "changeInfoProject";
    changeInfo.submitted = Timestamp.from(
        LocalDateTime.of(2022, 8, 10, 17, 25).toInstant(ZoneOffset.ofHours(3)));
    changeInfo.updated = Timestamp.from(
        LocalDateTime.of(2022, 8, 10, 17, 35).toInstant(ZoneOffset.ofHours(3)));
    changeInfo.owner = new AccountInfo(1);
    changeInfo.owner.username = "changeInfoOwnerUsername";
    changeInfo.mergeable = true;
    changeInfo.labels = Map.of("label1", new LabelInfo(), "label2", new LabelInfo());
    changeInfo.labels.get("label1").approved = new AccountInfo(2);
    Mockito.when(gerritService.getMRList()).thenReturn(List.of(changeInfo));

    var actualVersionsList = managementService.getVersionsList();

    var expectedChangeInfoDto = ChangeInfoDetailedDto.builder()
        .id("changeInfoId")
        .number(1)
        .changeId("changeInfoChangeId")
        .branch("changeInfoBranch")
        .created(LocalDateTime.of(2022, 8, 10, 14, 15))
        .subject("changeInfoSubject")
        .description("changeInfoTopic")
        .project("changeInfoProject")
        .submitted(LocalDateTime.of(2022, 8, 10, 14, 25))
        .updated(LocalDateTime.of(2022, 8, 10, 14, 35))
        .owner("changeInfoOwnerUsername")
        .mergeable(true)
        .labels(Map.of("label1", true, "label2", false))
        .build();
    Assertions.assertThat(actualVersionsList)
        .hasSize(1)
        .element(0).isEqualTo(expectedChangeInfoDto);
  }

  @Test
  @SneakyThrows
  void getDetailsOfHeadMasterTest() {
    Mockito.when(config.getHeadBranch()).thenReturn("master");
    Mockito.when(jGitService.getFilesInPath("master", "path")).thenReturn(List.of("details"));

    var actualDetailsOfHeadMaster = managementService.getDetailsOfHeadMaster("path");
    Assertions.assertThat(actualDetailsOfHeadMaster)
        .hasSize(1)
        .element(0).isEqualTo("details");
  }

  @Test
  @SneakyThrows
  void getVersionFileListTest() {
    var info1 = new FileInfo();
    info1.status = 'A';
    info1.size = 50;
    info1.linesInserted = 1;
    info1.linesDeleted = 0;
    info1.sizeDelta = 50;
    var info2 = new FileInfo();
    info2.status = null;
    info2.size = 33;
    info2.linesInserted = 2;
    info2.linesDeleted = 3;
    info2.sizeDelta = 22;
    Mockito.when(gerritService.getListOfChangesInMR("3"))
        .thenReturn(Map.of("file1", info1, "file2", info2));

    var resultList = managementService.getVersionFileList("3");

    var expectedFirstVersionedFileInfoDto = VersionedFileInfo.builder()
        .name("file1")
        .status("A")
        .size(50)
        .lineInserted(1)
        .lineDeleted(0)
        .sizeDelta(50)
        .build();
    var expectedFirstVersionedFileInfo = new Condition<>(expectedFirstVersionedFileInfoDto::equals,
        "equals to expected file info - ", expectedFirstVersionedFileInfoDto);
    var expectedSecondVersionedFileInfoDto = VersionedFileInfo.builder()
        .name("file2")
        .status(null)
        .size(33)
        .lineInserted(2)
        .lineDeleted(3)
        .sizeDelta(22)
        .build();
    var expectedSecondVersionedFileInfo = new Condition<>(
        expectedSecondVersionedFileInfoDto::equals,
        "equals to expected file info - ", expectedSecondVersionedFileInfoDto);
    Assertions.assertThat(resultList)
        .hasSize(2)
        .areAtLeastOne(expectedFirstVersionedFileInfo)
        .areAtLeastOne(expectedSecondVersionedFileInfo);
  }

  @Test
  @SneakyThrows
  void createNewVersionTest() {
    var createVersionRequest = new CreateVersionRequest();
    createVersionRequest.setName("version");
    createVersionRequest.setDescription("description");
    Mockito.when(gerritService.createChanges(createVersionRequest)).thenReturn("changeId");

    var actualVersion = managementService.createNewVersion(createVersionRequest);

    Assertions.assertThat(actualVersion)
        .isEqualTo("changeId");
  }

  @Test
  @SneakyThrows
  void getVersionDetailsTest() {
    var changeInfo = new ChangeInfo();
    changeInfo._number = 1;
    changeInfo.owner = new AccountInfo(1);
    changeInfo.labels = Map.of();
    Mockito.when(gerritService.getMRByNumber("1")).thenReturn(changeInfo);

    var actualChangeInfoDetailedDto = managementService.getVersionDetails("1");

    var expectedChangeInfoDetailedDto = ChangeInfoDetailedDto.builder()
        .number(1)
        .owner(null)
        .labels(Map.of())
        .build();
    Assertions.assertThat(actualChangeInfoDetailedDto)
        .isEqualTo(expectedChangeInfoDetailedDto);
  }

  @Test
  @SneakyThrows
  void getVersionDetailsTest_notFound() {
    Mockito.when(gerritService.getMRByNumber("1")).thenReturn(null);

    Assertions.assertThatThrownBy(() -> managementService.getVersionDetails("1"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @SneakyThrows
  void getMasterInfo() {
    var changeInfo = new ChangeInfo();
    changeInfo.id = "changeInfoId";
    changeInfo._number = 1;
    changeInfo.changeId = "changeInfoChangeId";
    changeInfo.branch = "changeInfoBranch";
    changeInfo.created = Timestamp.from(
        LocalDateTime.of(2022, 8, 10, 17, 15).toInstant(ZoneOffset.ofHours(3)));
    changeInfo.subject = "changeInfoSubject";
    changeInfo.topic = "changeInfoTopic";
    changeInfo.project = "changeInfoProject";
    changeInfo.submitted = Timestamp.from(
        LocalDateTime.of(2022, 8, 10, 17, 25).toInstant(ZoneOffset.ofHours(3)));
    changeInfo.updated = null;
    changeInfo.owner = new AccountInfo(1);
    changeInfo.owner.username = "changeInfoOwnerUsername";
    changeInfo.mergeable = true;
    changeInfo.labels = Map.of("label1", new LabelInfo(), "label2", new LabelInfo());
    changeInfo.labels.get("label1").approved = new AccountInfo(2);
    Mockito.when(gerritService.getLastMergedMR()).thenReturn(changeInfo);

    var result = managementService.getMasterInfo();

    var expectedChangeInfoDto = ChangeInfoDetailedDto.builder()
        .id("changeInfoId")
        .number(1)
        .changeId("changeInfoChangeId")
        .branch("changeInfoBranch")
        .created(LocalDateTime.of(2022, 8, 10, 14, 15))
        .subject("changeInfoSubject")
        .description("changeInfoTopic")
        .project("changeInfoProject")
        .submitted(LocalDateTime.of(2022, 8, 10, 14, 25))
        .updated(null)
        .owner("changeInfoOwnerUsername")
        .mergeable(true)
        .labels(Map.of("label1", true, "label2", false))
        .build();
    Assertions.assertThat(result)
        .isEqualTo(expectedChangeInfoDto);
  }

  @Test
  @SneakyThrows
  void getMasterInfo_null() {
    Mockito.when(gerritService.getLastMergedMR()).thenReturn(null);

    var result = managementService.getMasterInfo();

    Assertions.assertThat(result)
        .isNull();
    Mockito.verify(gerritService).getLastMergedMR();
  }

  @Test
  @SneakyThrows
  void getVersionChanges() {

  }

  @Test
  @SneakyThrows
  void rebaseTest() {
    final String changeId = "1";
    Mockito.doNothing().when(gerritService).rebase(changeId);
    managementService.rebase(changeId);
    Mockito.verify(gerritService).rebase(changeId);
  }
}
