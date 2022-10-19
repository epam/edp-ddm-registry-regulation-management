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

package com.epam.digital.data.platform.management;

import static com.epam.digital.data.platform.management.util.InitialisationUtils.createProcessXml;
import static com.epam.digital.data.platform.management.util.InitialisationUtils.initChangeInfo;
import static com.epam.digital.data.platform.management.util.InitialisationUtils.initChangeInfoDto;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

import com.epam.digital.data.platform.management.config.JacksonConfig;
import com.epam.digital.data.platform.management.model.dto.BusinessProcessDetailsShort;
import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.epam.digital.data.platform.management.util.InitialisationUtils;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.RevisionInfo;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

public class CandidateVersionBPControllerIT extends BaseIT {

  private static final String BASE_REQUEST = "/versions/candidates/{versionCandidateId}/business-processes";

  @Test
  @SneakyThrows
  public void getBusinessProcess() {
    final var versionCandidateNumber = new Random().nextInt(Integer.MAX_VALUE);
    final var versionCandidateId = String.valueOf(versionCandidateNumber);
    final var bpName = RandomString.make();
    final var bpTitle = RandomString.make();
    final var bpmnPath = String.format("bpmn/%s.bpmn", bpName);
    final var bpContent = String.format(testProcessFormat, bpName, bpTitle);

    final var changeInfo = initChangeInfo(versionCandidateNumber);
    final var changeInfoDto = initChangeInfoDto(changeInfo);

    final var versionCandidateCloneResult = jGitWrapperMock.mockCloneCommand(versionCandidateId);
    gerritApiMock.mockGetChangeInfo(versionCandidateId, changeInfo);
    final var openRepoResult = jGitWrapperMock.mockOpenGit(versionCandidateId);
    final var fetchCommand = jGitWrapperMock.mockFetchCommand(openRepoResult, changeInfoDto);
    final var checkoutCommand = jGitWrapperMock.mockCheckoutCommand(openRepoResult);
    jGitWrapperMock.mockGetFileContent(openRepoResult, bpmnPath, bpContent);

    mockMvc.perform(get(BASE_REQUEST + "/{businessProcessName}", versionCandidateId, bpName)
            .accept(MediaType.TEXT_XML))
        .andExpectAll(
            status().isOk(),
            content().contentType("text/xml"),
            xpath("/bpmn:definitions/bpmn:process/@id", BPMN_NAMESPACES).string(bpName),
            xpath("/bpmn:definitions/bpmn:process/@name", BPMN_NAMESPACES).string(bpTitle)
        );

    Mockito.verify(versionCandidateCloneResult).close();
    Mockito.verify(fetchCommand).call();
    Mockito.verify(checkoutCommand).call();
  }

  @Test
  @SneakyThrows
  public void getBusinessProcessesByVersionId() {
    final var expectedBp1 = BusinessProcessDetailsShort.builder()
        .name(RandomString.make())
        .title(RandomString.make())
        .created(LocalDateTime.of(2022, 10, 12, 23, 35))
        .updated(LocalDateTime.of(2022, 10, 19, 17, 16))
        .build();
    final var expectedBp2 = BusinessProcessDetailsShort.builder()
        .name(RandomString.make())
        .title(RandomString.make())
        .created(LocalDateTime.of(2022, 10, 13, 11, 37, 11, 446000000))
        .updated(LocalDateTime.of(2022, 10, 15, 6, 27, 56, 523000000))
        .build();

    final var versionCandidateNumber = new Random().nextInt(Integer.MAX_VALUE);
    final var versionCandidateId = String.valueOf(versionCandidateNumber);
    final var bpmnPath = "bpmn";

    final var bp1FileName = String.format("%s.bpmn", expectedBp1.getName());
    final var bpmnFile1Path = String.format("%s/%s", bpmnPath, bp1FileName);
    final var bpmnFile1Content = String.format(testProcessFormat, expectedBp1.getName(),
        expectedBp1.getTitle());

    final var bp2FileName = String.format("%s.bpmn", expectedBp2.getName());
    final var bpmnFile2Path = String.format("%s/%s", bpmnPath, bp2FileName);
    final var bpmnFile2Content = String.format(testProcessFormatWithDates,
        JacksonConfig.DATE_TIME_FORMATTER.format(expectedBp2.getCreated()),
        JacksonConfig.DATE_TIME_FORMATTER.format(expectedBp2.getUpdated()),
        expectedBp2.getName(), expectedBp2.getTitle());

    final var changeInfo = initChangeInfo(versionCandidateNumber);
    final var changeInfoDto = initChangeInfoDto(changeInfo);

    final var versionCandidateCloneResult = jGitWrapperMock.mockCloneCommand(versionCandidateId);
    gerritApiMock.mockGetChangeInfo(versionCandidateId, changeInfo);
    final var openRepoResult = jGitWrapperMock.mockOpenGit(versionCandidateId);
    final var fetchCommand = jGitWrapperMock.mockFetchCommand(openRepoResult, changeInfoDto);
    final var checkoutCommand = jGitWrapperMock.mockCheckoutCommand(openRepoResult);
    jGitWrapperMock.mockGetFileList(openRepoResult, bpmnPath, List.of(bp1FileName, bp2FileName));
    jGitWrapperMock.mockGitFileDates(openRepoResult, bpmnFile1Path, expectedBp1.getCreated(),
        expectedBp1.getUpdated());
    jGitWrapperMock.mockGitFileDates(openRepoResult, bpmnFile2Path, LocalDateTime.now(),
        LocalDateTime.now());
    jGitWrapperMock.mockGetFileContent(openRepoResult, bpmnFile1Path, bpmnFile1Content);
    jGitWrapperMock.mockGetFileContent(openRepoResult, bpmnFile2Path, bpmnFile2Content);

    final var expectedList = Stream.of(expectedBp1, expectedBp2)
        .sorted(Comparator.comparing(BusinessProcessDetailsShort::getName))
        .collect(Collectors.toList());

    mockMvc.perform(get(BASE_REQUEST, versionCandidateId)
        .accept(MediaType.APPLICATION_JSON)).andExpectAll(
        status().isOk(),
        content().contentType(MediaType.APPLICATION_JSON),
        content().json(objectMapper.writeValueAsString(expectedList), true));

    Mockito.verify(versionCandidateCloneResult).close();
    Mockito.verify(fetchCommand).call();
    Mockito.verify(checkoutCommand).call();
  }

  @Test
  @SneakyThrows
  public void getBusinessProcessesByVersionId_NoBusinessProcesses() {
    final var versionCandidateId = RandomString.make();
    String businessProcessName = "businessProcessName";
    ChangeInfo changeInfo = initChangeInfo(1, "admin", "admin@epam.com", "admin");
    ChangeInfoDto changeInfoDto = initChangeInfoDto(versionCandidateId);

    changeInfo.revisions = new HashMap<>();
    RevisionInfo revisionInfo = new RevisionInfo();
    revisionInfo.ref = "-1";
    changeInfo.revisions.put(businessProcessName, revisionInfo);
    changeInfo.currentRevision = businessProcessName;
    changeInfoDto.setRefs(versionCandidateId);

    final var versionCandidateCloneResult = jGitWrapperMock.mockCloneCommand(versionCandidateId);
    jGitWrapperMock.mockGetBusinessProcessList(Map.of());
    jGitWrapperMock.mockFetchCommand(changeInfoDto);
    jGitWrapperMock.mockCheckoutCommand();
    jGitWrapperMock.mockPullCommand();
    gerritApiMock.mockGetChangeInfo(versionCandidateId, changeInfo);
    mockMvc.perform(get(BASE_REQUEST, versionCandidateId)
        .accept(MediaType.APPLICATION_JSON_VALUE)).andExpectAll(
        status().isOk(),
        content().contentType("application/json"),
        jsonPath("$", hasSize(0))
    );

    Mockito.verify(versionCandidateCloneResult).close();
  }

  @Disabled
  @Test
  @SneakyThrows
  public void createBusinessProcess() {
    //todo fix this test
    String versionCandidateId = "id1";
    String businessProcessName = "name";
    ChangeInfo changeInfo = initChangeInfo(1, "admin", "admin@epam.com", "admin");
    ChangeInfoDto changeInfoDto = initChangeInfoDto(versionCandidateId);
    changeInfo.revisions = new HashMap<>();
    RevisionInfo revisionInfo = new RevisionInfo();
    revisionInfo.ref = "id1";
    changeInfo.revisions.put(businessProcessName, revisionInfo);
    changeInfo.currentRevision = businessProcessName;
    changeInfoDto.setRefs(versionCandidateId);
    gerritApiMock.mockGetChangeInfo(versionCandidateId, changeInfo);
    final var versionCandidateCloneResult = jGitWrapperMock.mockCloneCommand(versionCandidateId);
    jGitWrapperMock.mockCheckoutCommand();
    jGitWrapperMock.mockFetchCommand(changeInfoDto);
    jGitWrapperMock.mockPullCommand();
    jGitWrapperMock.mockLogCommand();
    jGitWrapperMock.mockAddCommand();
    jGitWrapperMock.mockStatusCommand();
    jGitWrapperMock.mockRemoteAddCommand();
    jGitWrapperMock.mockPushCommand();
    jGitWrapperMock.mockCommitCommand();
    jGitWrapperMock.mockGetBusinessProcess(businessProcess);
    jGitWrapperMock.mockGetBusinessProcessList(Map.of());
    mockMvc.perform(MockMvcRequestBuilders.post(
            BASE_REQUEST + "/{businessProcessName}", versionCandidateId, businessProcessName)
        .contentType(MediaType.TEXT_XML).content(businessProcess)
        .accept(MediaType.TEXT_XML)).andExpectAll(
        status().isCreated(),
        content().contentType("text/xml"),
        xpath("/bpmn:definitions/bpmn:process/@id", BPMN_NAMESPACES).string("name"),
        xpath("/bpmn:definitions/bpmn:process/@name", BPMN_NAMESPACES).string("title"));

    Mockito.verify(versionCandidateCloneResult).close();
  }

  @Test
  @SneakyThrows
  public void updateBusinessProcess() {
    final var versionCandidateId = RandomString.make();
    String businessProcessName = "businessProcessName";
    ChangeInfo changeInfo = initChangeInfo(1, "admin", "admin@epam.com", "admin");
    ChangeInfoDto changeInfoDto = initChangeInfoDto(versionCandidateId);

    changeInfo.revisions = new HashMap<>();
    RevisionInfo revisionInfo = new RevisionInfo();
    revisionInfo.ref = versionCandidateId;
    changeInfo.revisions.put(businessProcessName, revisionInfo);
    changeInfo.currentRevision = businessProcessName;
    changeInfoDto.setRefs(versionCandidateId);

    InitialisationUtils.createTempRepo(versionCandidateId);
    gerritApiMock.mockGetChangeInfo(versionCandidateId, changeInfo);
    jGitWrapperMock.mockCheckoutCommand();
    jGitWrapperMock.mockPullCommand();
    createProcessXml(businessProcess, versionCandidateId, businessProcessName);
    jGitWrapperMock.mockFetchCommand(changeInfoDto);
    jGitWrapperMock.mockAddCommand();
    jGitWrapperMock.mockStatusCommand();
    jGitWrapperMock.mockRemoteAddCommand();
    jGitWrapperMock.mockPushCommand();
    jGitWrapperMock.mockCommitCommand();
    jGitWrapperMock.mockLogCommand();
    jGitWrapperMock.mockGetBusinessProcess(businessProcess);
    jGitWrapperMock.mockGetBusinessProcessList(Map.of("name", businessProcess));
    mockMvc.perform(MockMvcRequestBuilders.put(
            BASE_REQUEST + "/{businessProcessName}", versionCandidateId, businessProcessName)
        .contentType(MediaType.TEXT_XML).content(businessProcess)
        .accept(MediaType.TEXT_XML)).andExpectAll(
        status().isOk(),
        content().contentType("text/xml"),
        xpath("/bpmn:definitions/bpmn:process/@id", BPMN_NAMESPACES).string("name"),
        xpath("/bpmn:definitions/bpmn:process/@name", BPMN_NAMESPACES).string("title"));
  }

  @Test
  @SneakyThrows
  public void deleteBusinessProcess() {
    final var versionCandidateId = RandomString.make();
    String businessProcessName = "businessProcessName";
    ChangeInfo changeInfo = initChangeInfo(1, "admin", "admin@epam.com", "admin");
    ChangeInfoDto changeInfoDto = initChangeInfoDto(versionCandidateId);
    changeInfo.revisions = new HashMap<>();
    RevisionInfo revisionInfo = new RevisionInfo();
    revisionInfo.ref = "-1";
    changeInfo.revisions.put(businessProcessName, revisionInfo);
    changeInfo.currentRevision = businessProcessName;
    changeInfoDto.setRefs(revisionInfo.ref);
    gerritApiMock.mockGetChangeInfo(versionCandidateId, changeInfo);
    final var versionCandidateCloneResult = jGitWrapperMock.mockCloneCommand(versionCandidateId);
    jGitWrapperMock.mockGetBusinessProcessList(Map.of(businessProcessName, businessProcess));
    jGitWrapperMock.mockLogCommand();
    jGitWrapperMock.mockCheckoutCommand();
    jGitWrapperMock.mockPullCommand();
    jGitWrapperMock.mockFetchCommand(changeInfoDto);
    mockMvc.perform(MockMvcRequestBuilders.delete(
            BASE_REQUEST + "/{businessProcessName}", versionCandidateId, businessProcessName))
        .andExpect(status().isNoContent());

    Mockito.verify(versionCandidateCloneResult).close();
  }
}
