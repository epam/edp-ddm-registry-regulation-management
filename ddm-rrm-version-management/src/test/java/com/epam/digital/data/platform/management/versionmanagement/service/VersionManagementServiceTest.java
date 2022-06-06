/*
 * Copyright 2023 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.management.versionmanagement.service;

import static org.mockito.ArgumentMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.epam.digital.data.platform.management.gerritintegration.exception.GerritChangeNotFoundException;
import com.epam.digital.data.platform.management.gerritintegration.model.ChangeInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.model.ChangeInfoShortDto;
import com.epam.digital.data.platform.management.gerritintegration.model.CreateChangeInputDto;
import com.epam.digital.data.platform.management.gerritintegration.model.FileInfoDto;
import com.epam.digital.data.platform.management.versionmanagement.model.VersionInfoDto;
import com.epam.digital.data.platform.management.versionmanagement.model.VersionInfoShortDto;
import com.epam.digital.data.platform.management.versionmanagement.model.VersionedFileInfoDto;

import lombok.SneakyThrows;

class VersionManagementServiceTest extends VersionManagementServiceBaseTest {

  @Test
  @SneakyThrows
  void getVersionsListTest() {
    var changeInfo = new ChangeInfoShortDto();
    changeInfo.setNumber("1");
    changeInfo.setSubject("changeInfoSubject");
    changeInfo.setTopic("changeInfoTopic");

    Mockito.when(gerritService.getMRList()).thenReturn(List.of(changeInfo));

    var actualVersionsList = managementService.getVersionsList();

    var expectedChangeInfoDto = VersionInfoShortDto.builder()
        .number(1)
        .subject("changeInfoSubject")
        .description("changeInfoTopic")
        .build();
    Assertions.assertThat(actualVersionsList)
        .hasSize(1)
        .element(0).isEqualTo(expectedChangeInfoDto);
  }

  @Test
  @SneakyThrows
  void declineTest() {
    var changeId = RandomString.make();
    Mockito.doNothing().when(gerritService).declineChange(changeId);
    Assertions.assertThatCode(() -> managementService.decline(changeId))
        .doesNotThrowAnyException();

    Mockito.verify(gerritService).declineChange(changeId);
  }

  @Test
  @SneakyThrows
  void markReviewedTest() {
    var changeId = RandomString.make();
    Mockito.when(gerritService.review(changeId)).thenReturn(true);

    Assertions.assertThatCode(() -> managementService.markReviewed(changeId))
        .doesNotThrowAnyException();
    Mockito.verify(gerritService).review(changeId);
    Assertions.assertThat(managementService.markReviewed(changeId)).isTrue();
  }

  @Test
  @SneakyThrows
  void submitTest() {
    var changeId = RandomString.make();
    Mockito.doNothing().when(gerritService).submitChanges(changeId);
    Mockito.doReturn("master").when(gerritPropertiesConfig).getHeadBranch();
    Assertions.assertThatCode(() -> managementService.submit(changeId))
        .doesNotThrowAnyException();

    Mockito.verify(gerritService).submitChanges(changeId);
    Mockito.verify(cacheService).clearCatalogCache("master");
  }

  @Test
  @SneakyThrows
  void getVersionFileListTest() {
    var info1 = new FileInfoDto();
    info1.setStatus("A");
    info1.setSize(50);
    info1.setLinesInserted(1);
    info1.setLinesDeleted(0);
    info1.setSizeDelta(50);
    var info2 = new FileInfoDto();
    info2.setStatus(null);
    info2.setSize(33);
    info2.setLinesInserted(2);
    info2.setLinesDeleted(3);
    info2.setSizeDelta(22);
    Mockito.when(gerritService.getListOfChangesInMR("3"))
        .thenReturn(Map.of("file1", info1, "file2", info2));

    var resultList = managementService.getVersionFileList("3");

    var expectedFirstVersionedFileInfoDto = VersionedFileInfoDto.builder()
        .name("file1")
        .status("A")
        .size(50)
        .lineInserted(1)
        .lineDeleted(0)
        .sizeDelta(50)
        .build();
    var expectedFirstVersionedFileInfo = new Condition<>(expectedFirstVersionedFileInfoDto::equals,
        "equals to expected file info - ", expectedFirstVersionedFileInfoDto);
    var expectedSecondVersionedFileInfoDto = VersionedFileInfoDto.builder()
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
    final var versionName = RandomString.make();
    final var versionDescription = RandomString.make();
    final var versionNumber = RandomString.make();

    final var createChangeInputDto = CreateChangeInputDto.builder()
        .name(versionName)
        .description(versionDescription)
        .build();
    Mockito.when(gerritService.createChanges(createChangeInputDto)).thenReturn(versionNumber);

    final var createVersion = CreateChangeInputDto.builder()
        .name(versionName)
        .description(versionDescription)
        .build();
    Mockito.when(gerritService.createChanges(createChangeInputDto)).thenReturn(versionNumber);

    Mockito.doNothing().when(publisher).publishVersionCandidateCreatedEvent(versionNumber);

    final var actualVersion = managementService.createNewVersion(createVersion);

    Assertions.assertThat(actualVersion)
        .isEqualTo(versionNumber);

    Mockito.verify(gerritService).createChanges(createChangeInputDto);
    Mockito.verify(publisher).publishVersionCandidateCreatedEvent(versionNumber);
  }

  @Test
  @SneakyThrows
  void getVersionDetailsTest() {
    var changeInfo = new ChangeInfoDto();
    changeInfo.setNumber("1");
    changeInfo.setOwner("owner");
    changeInfo.setLabels(Map.of());
    Mockito.when(gerritService.getMRByNumber("1")).thenReturn(changeInfo);

    var actualChangeInfoDetailedDto = managementService.getVersionDetails("1");

    var expectedChangeInfoDetailedDto = VersionInfoDto.builder()
        .number(1)
        .owner("owner")
        .labels(Map.of())
        .build();
    Assertions.assertThat(actualChangeInfoDetailedDto)
        .isEqualTo(expectedChangeInfoDetailedDto);
  }

  @Test
  @SneakyThrows
  void getVersionDetailsTest_notFound() {
    var changeNumber = RandomString.make();
    Mockito.when(gerritService.getMRByNumber(changeNumber)).thenReturn(null);

    Assertions.assertThatThrownBy(() -> managementService.getVersionDetails(changeNumber))
        .isInstanceOf(GerritChangeNotFoundException.class);
  }

  @Test
  @SneakyThrows
  void getMasterInfo() {
    var changeInfo = new ChangeInfoDto();
    changeInfo.setId("changeInfoId");
    changeInfo.setNumber("1");
    changeInfo.setChangeId("changeInfoChangeId");
    changeInfo.setBranch("changeInfoBranch");
    changeInfo.setCreated(
        LocalDateTime.of(2022, 8, 10, 14, 15));
    changeInfo.setSubject("changeInfoSubject");
    changeInfo.setTopic("changeInfoTopic");
    changeInfo.setProject("changeInfoProject");
    changeInfo.setSubmitted(
        LocalDateTime.of(2022, 8, 10, 14, 25));
    changeInfo.setUpdated(null);
    changeInfo.setOwner("changeInfoOwnerUsername");
    changeInfo.setMergeable(true);
    changeInfo.setLabels(Map.of("label1", 1, "label2", -1));
    Mockito.when(gerritService.getLastMergedMR()).thenReturn(changeInfo);

    var result = managementService.getMasterInfo();

    var expectedChangeInfoDto = VersionInfoDto.builder()
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
        .labels(Map.of("label1", 1, "label2", -1))
        .build();
    Assertions.assertThat(result).isEqualTo(expectedChangeInfoDto);
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
    final var version = RandomString.make();
    final var changeInfo = new ChangeInfoDto();
    changeInfo.setId("changeInfoId");
    changeInfo.setNumber("1");
    changeInfo.setChangeId("changeInfoChangeId");
    changeInfo.setBranch("changeInfoBranch");
    changeInfo.setCreated(
        LocalDateTime.of(2022, 8, 10, 14, 15));
    changeInfo.setSubject("changeInfoSubject");
    changeInfo.setTopic("changeInfoTopic");
    changeInfo.setProject("changeInfoProject");
    changeInfo.setSubmitted(
        LocalDateTime.of(2022, 8, 10, 14, 25));
    changeInfo.setUpdated(
        LocalDateTime.of(2022, 8, 10, 14, 35));
    changeInfo.setOwner("changeInfoOwnerUsername");
    changeInfo.setMergeable(true);
    changeInfo.setLabels(Map.of("label1", -1, "label2", -1));
    Mockito.when(gerritService.getMRByNumber(version)).thenReturn(changeInfo);
    var expected = VersionInfoDto.builder()
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
        .labels(Map.of("label1", -1, "label2", -1))
        .build();

    var actual = managementService.getVersionDetails(version);
    Assertions.assertThat(actual).isEqualTo(expected);
  }

  @Test
  @SneakyThrows
  void rebaseTest() {
    final var version = RandomString.make();
    final var changeId = RandomString.make();
    final var refs = RandomString.make();

    final var changeInfo = new ChangeInfoDto();
    changeInfo.setChangeId(changeId);
    Mockito.when(gerritService.getMRByNumber(version)).thenReturn(changeInfo);

    Mockito.doNothing().when(gerritService).rebase(changeId);

    final var changeInfoDto = new ChangeInfoDto();
    changeInfoDto.setChangeId(changeId);
    changeInfoDto.setRefs(refs);
    changeInfoDto.setNumber(version);
    changeInfoDto.setMergeable(false);
    Mockito.when(gerritService.getChangeInfo(changeId)).thenReturn(changeInfoDto);

    Mockito.doNothing().when(jGitService).fetch(version, refs);

    managementService.rebase(version);

    Mockito.verify(gerritService).getMRByNumber(version);
    Mockito.verify(gerritService).rebase(changeId);
    Mockito.verify(gerritService).getChangeInfo(changeId);
    Mockito.verify(jGitService).fetch(version, refs);
    Mockito.verify(jGitService).getConflicts(version);
    Mockito.verify(cacheService).clearCatalogCache(version);
    Mockito.verify(cacheService).updateConflictsCache(eq(version), anyList());
    Mockito.verify(cacheService).updateLatestRebaseCache(eq(version), any());
  }
}
