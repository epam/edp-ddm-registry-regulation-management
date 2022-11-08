/*
 * Copyright 2022 EPAM Systems.
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

package com.epam.digital.data.platform.management.gerritintegration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.gerritintegration.exception.GerritChangeNotFoundException;
import com.epam.digital.data.platform.management.gerritintegration.mapper.GerritMapper;
import com.epam.digital.data.platform.management.gerritintegration.model.ChangeInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.model.CreateChangeInputDto;
import com.epam.digital.data.platform.management.gerritintegration.model.FileInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.model.RobotCommentInputDto;
import com.google.gerrit.extensions.api.changes.ChangeApi;
import com.google.gerrit.extensions.api.changes.Changes;
import com.google.gerrit.extensions.api.changes.RebaseInput;
import com.google.gerrit.extensions.api.changes.ReviewResult;
import com.google.gerrit.extensions.api.changes.RevisionApi;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.MergeableInfo;
import com.google.gerrit.extensions.common.RevisionInfo;
import com.google.gson.Gson;
import com.urswolfer.gerrit.client.rest.GerritApiImpl;
import com.urswolfer.gerrit.client.rest.http.GerritRestClient;
import com.urswolfer.gerrit.client.rest.http.HttpStatusException;
import com.urswolfer.gerrit.client.rest.http.changes.ChangeApiRestClient;
import com.urswolfer.gerrit.client.rest.http.changes.ChangesRestClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GerritServiceTest {

  @Mock
  public GerritPropertiesConfig gerritPropertiesConfig;
  @Mock
  public GerritApiImpl gerritApiImpl;
  @Spy
  private GerritMapper mapper = Mappers.getMapper(GerritMapper.class);
  @InjectMocks
  private GerritServiceImpl gerritService;
  @Mock
  private ChangesRestClient changes;
  @Mock
  private ChangeApiRestClient changeApiRestClient;
  @Mock
  private Changes.QueryRequest request;
  @Mock
  private RevisionApi revisionApi;
  @Mock
  private GerritRestClient gerritRestClient;
  List<ChangeInfo> changeInfos = new ArrayList<>();
  ChangeInfo changeInfo = new ChangeInfo();

  @BeforeEach
  void initChanges() {
    changeInfo._number = 5;
    changeInfos.add(changeInfo);
    Mockito.lenient().when(gerritApiImpl.changes()).thenReturn(changes);
    ReflectionTestUtils.setField(gerritService, "gerritMapper", mapper);
  }

  @Test
  @SneakyThrows
  void getTopicFromChange() {
    String changeId = "1";
    String expected = "testTopic1";
    ChangeApi changeApi = Mockito.mock(ChangeApi.class);
    Mockito.when(changes.id(changeId)).thenReturn(changeApi);
    Mockito.when(changeApi.topic()).thenReturn(expected);
    String actual = gerritService.getTopic(changeId);
    Assertions.assertEquals(expected, actual);
  }

  @Test
  @SneakyThrows
  void writeAndGetTopicFromChangeTest() {
    String topic1 = "testTopic1";
    String topic2 = "testTopic2";

    ChangeApi changeApi = Mockito.mock(ChangeApi.class);
    Mockito.when(changes.id(any())).thenReturn(changeApi);

    ArgumentCaptor<String> valueCapture = ArgumentCaptor.forClass(String.class);
    Mockito.when(changeApi.topic()).thenAnswer(e -> valueCapture.getValue());
    Mockito.doNothing().when(changeApi).topic(valueCapture.capture());

    gerritService.setTopic(topic1, "");
    Assertions.assertEquals(topic1, valueCapture.getValue());
    String rTopic1 = gerritService.getTopic("");
    gerritService.setTopic(topic2, "");
    Assertions.assertEquals(topic2, valueCapture.getValue());
    String rTopic2 = gerritService.getTopic("");

    Assertions.assertEquals(topic1, rTopic1);
    Assertions.assertEquals(topic2, rTopic2);
  }

  @Test
  @SneakyThrows
  void getMRByNumberTest() {
    Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn("");
    String versionNumber = "10";
    String testVersion = "project:+" + versionNumber;
    ChangeInfo info = new ChangeInfo();
    info._number = 10;
    info.mergeable = true;
    Mockito.when(changes.query(eq(testVersion))).thenReturn(request);
    ChangeApi changeApi = Mockito.mock(ChangeApi.class);
    RevisionApi revisionApi = Mockito.mock(RevisionApi.class);
    MergeableInfo mergeableInfo = Mockito.mock(MergeableInfo.class);
    mergeableInfo.mergeable = false;
    Mockito.when(changes.id(any())).thenReturn(changeApi);
    Mockito.when(changeApi.get()).thenReturn(info);
    Mockito.when(changeApi.current()).thenReturn(revisionApi);
    Mockito.when(revisionApi.mergeable()).thenReturn(mergeableInfo);
    Mockito.when(request.get()).thenReturn(changeInfos);
    var dto = new ChangeInfoDto();
    dto.setNumber("10");
    Assertions.assertTrue(info.mergeable);
    ChangeInfoDto c = gerritService.getMRByNumber(versionNumber);
    Assertions.assertFalse(c.getMergeable());
    Assertions.assertEquals(dto.getNumber(), c.getNumber());
  }

  @Test
  @SneakyThrows
  void getMRByNumberNullableTest() {
    Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn("");
    String versionNumber = "10";
    String testVersion = "project:+" + versionNumber;
    ChangeInfo info = new ChangeInfo();
    info._number = 10;
    info.mergeable = true;
    Mockito.when(changes.query(eq(testVersion))).thenReturn(request);
    ChangeApi changeApi = Mockito.mock(ChangeApi.class);
    RevisionApi revisionApi = Mockito.mock(RevisionApi.class);
    MergeableInfo mergeableInfo = Mockito.mock(MergeableInfo.class);
    mergeableInfo.mergeable = false;
    Mockito.when(request.get()).thenReturn(new ArrayList<>());
    Assertions.assertThrows(GerritChangeNotFoundException.class, () -> gerritService.getMRByNumber(versionNumber));
  }

  @Test
  @SneakyThrows
  void getMrListNotNullTest() {
    Mockito.when(changes.query(any())).thenReturn(request);
    Mockito.when(request.get()).thenReturn(new ArrayList<>());
    List<ChangeInfoDto> mrList = gerritService.getMRList();
    Assertions.assertNotNull(mrList);
  }

  @Test
  @SneakyThrows
  void etMrListNotEmptyTest() {
    final ChangeInfoDto dto = new ChangeInfoDto();
    dto.setNumber("5");
    Mockito.when(changes.query(any())).thenReturn(request);
    Mockito.when(request.get()).thenReturn(changeInfos);
    Mockito.when(changes.id(any())).thenReturn(changeApiRestClient);
    Mockito.when(changeApiRestClient.get()).thenReturn(changeInfo);
    Mockito.when(changeApiRestClient.current()).thenReturn(revisionApi);
    Mockito.when(revisionApi.mergeable()).thenReturn(new MergeableInfo());
    List<ChangeInfoDto> mrList = gerritService.getMRList();
    Assertions.assertNotNull(mrList);
    ChangeInfoDto changeInfo = mrList.get(0);
    Assertions.assertEquals("5", changeInfo.getNumber());
  }

  @Test
  @SneakyThrows
  void getChangeInfoTest() {
    RevisionInfo revisionInfo = new RevisionInfo();
    revisionInfo.ref = "refs";
    HashMap<String, RevisionInfo> revisionsMap = new HashMap<>();
    revisionsMap.put(null, revisionInfo);
    changeInfo.revisions = revisionsMap;
    changeInfo.changeId = "changeId";
    Mockito.when(changes.id(any())).thenReturn(changeApiRestClient);
    Mockito.when(changeApiRestClient.get()).thenReturn(changeInfo);

    var changeInfoDto = gerritService.getChangeInfo("changeId");
    Assertions.assertEquals("changeId", changeInfoDto.getChangeId());
    Assertions.assertEquals("refs", changeInfoDto.getRefs());
    Assertions.assertEquals("5", changeInfoDto.getNumber());
  }

  @Test
  @SneakyThrows
  void getMrChangesListTest() {
    Mockito.when(changes.id(any())).thenReturn(changeApiRestClient);
    Mockito.when(changeApiRestClient.current()).thenReturn(revisionApi);
    Mockito.when(revisionApi.files()).thenReturn(new HashMap<>());
    Map<String, FileInfoDto> files = gerritService.getListOfChangesInMR("changeId");
    Assertions.assertNotNull(files);
  }

  @Test
  @SneakyThrows
  void getFileContentTest() {
    String fileContent = gerritService.getFileContent(null, "");
    Assertions.assertNull(fileContent);
  }

  @Test
  @SneakyThrows
  void createChangesTest() {
    Mockito.when(changes.create(any())).thenReturn(changeApiRestClient);
    Mockito.when(changeApiRestClient.get()).thenReturn(changeInfo);

    var request = new CreateChangeInputDto();
    request.setName("name");
    request.setDescription("description");
    String change = gerritService.createChanges(request);
    Assertions.assertNotNull(change);
    Assertions.assertEquals("5", change);
  }

  @Test
  @SneakyThrows
  void reviewTest() {
    ReviewResult reviewResult = new ReviewResult();
    reviewResult.ready = true;
    Mockito.when(changes.id(any())).thenReturn(changeApiRestClient);
    Mockito.when(changeApiRestClient.current()).thenReturn(revisionApi);
    Mockito.when(revisionApi.review(any())).thenReturn(reviewResult);
    Boolean review = gerritService.review("changeId");
    Assertions.assertNotNull(review);
  }


  @Test
  @SneakyThrows
  void declineChangeTest() {
    Mockito.when(changes.id(any())).thenReturn(changeApiRestClient);
    gerritService.declineChange("changeId");
    Mockito.verify(changeApiRestClient, Mockito.times(1)).abandon();
  }

  @Test
  @SneakyThrows
  void robotCommentTest() {
    Mockito.when(changes.id(any())).thenReturn(changeApiRestClient);
    Mockito.when(changeApiRestClient.current()).thenReturn(revisionApi);
    Mockito.when(revisionApi.review(any())).thenReturn(new ReviewResult());
    gerritService.rebase(null);
    RobotCommentInputDto requestDto = new RobotCommentInputDto();
    requestDto.setComment("Comment");
    gerritService.robotComment(requestDto, "changeId");
    Mockito.verify(revisionApi, Mockito.times(1)).review(any());
  }

  @Test
  @SneakyThrows
  void robotCommentTestEmptyComment() {
    Mockito.when(changes.id(any())).thenReturn(changeApiRestClient);
    Mockito.when(changeApiRestClient.current()).thenReturn(revisionApi);
    Mockito.when(revisionApi.review(any())).thenReturn(new ReviewResult());
    gerritService.rebase(null);
    RobotCommentInputDto requestDto = new RobotCommentInputDto();
    gerritService.robotComment(requestDto, "changeId");
    Mockito.verify(revisionApi, Mockito.times(1)).review(any());
  }

  @Test
  @SneakyThrows
  void getLastMergedMR() {
    Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn("repo");
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn("user");
    Mockito.when(changes.query("project:repo+status:merged+owner:user")).thenReturn(request);
    Mockito.when(request.withLimit(1)).thenReturn(request);
    Mockito.when(request.get()).thenReturn(changeInfos);
    Mockito.when(changes.id(changeInfo.id)).thenReturn(changeApiRestClient);
    Mockito.when(changeApiRestClient.get()).thenReturn(changeInfo);
    var dto = new ChangeInfoDto();
    dto.setNumber("5");
    var result = gerritService.getLastMergedMR();

    assertThat(result).hasFieldOrPropertyWithValue("number", dto.getNumber());

    Mockito.verify(request).get();
    Mockito.verify(request).withLimit(1);
    Mockito.verify(changes).query("project:repo+status:merged+owner:user");
    Mockito.verify(gerritPropertiesConfig).getRepository();
  }

  @Test
  @SneakyThrows
  void getLastMergedMR_noMergedMRs() {
    Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn("repo");
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn("user");
    Mockito.when(changes.query("project:repo+status:merged+owner:user")).thenReturn(request);
    Mockito.when(request.withLimit(1)).thenReturn(request);
    Mockito.when(request.get()).thenReturn(List.of());

    var result = gerritService.getLastMergedMR();

    Assertions.assertNull(result);

    Mockito.verify(request).get();
    Mockito.verify(request).withLimit(1);
    Mockito.verify(changes).query("project:repo+status:merged+owner:user");
    Mockito.verify(gerritPropertiesConfig).getRepository();
  }

  @Test
  @SneakyThrows
  void rebaseTest() {
    String changeId = "1";
    String request = String.format("/changes/%s/rebase", changeId);
    Mockito.when(gerritApiImpl.restClient()).thenReturn(gerritRestClient);
    final String requestBody = new Gson().toJson(new RebaseInput(), RebaseInput.class);
    Mockito.when(gerritRestClient.postRequest(request, requestBody)).thenReturn(null);

    org.assertj.core.api.Assertions.assertThatCode(() -> gerritService.rebase(changeId))
        .doesNotThrowAnyException();

    Mockito.verify(gerritRestClient).postRequest(request, requestBody);

  }

  @Test
  @SneakyThrows
  void rebaseConflictTest() {
    String changeId = "1";
    String request = String.format("/changes/%s/rebase", changeId);
    Mockito.when(gerritApiImpl.restClient()).thenReturn(gerritRestClient);
    final String requestBody = new Gson().toJson(new RebaseInput(), RebaseInput.class);
    Mockito.when(gerritRestClient.postRequest(request, requestBody)).thenThrow(
        new HttpStatusException(409, "", ""));

    org.assertj.core.api.Assertions.assertThatCode(() -> gerritService.rebase(changeId))
        .doesNotThrowAnyException();

    Mockito.verify(gerritRestClient).postRequest(request, requestBody);
  }
}
