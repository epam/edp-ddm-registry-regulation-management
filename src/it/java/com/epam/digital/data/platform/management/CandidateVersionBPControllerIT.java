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

import static com.epam.digital.data.platform.management.util.InitialisationUtils.initChangeInfo;
import static com.epam.digital.data.platform.management.util.InitialisationUtils.initChangeInfoDto;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

import com.epam.digital.data.platform.management.config.JacksonConfig;
import com.epam.digital.data.platform.management.model.dto.BusinessProcessDetailsShort;
import com.epam.digital.data.platform.management.util.InitialisationUtils;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.eclipse.jgit.transport.RefSpec;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.xml.sax.InputSource;
import org.xmlunit.builder.DiffBuilder;

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
    final var versionCandidateNumber = new Random().nextInt(Integer.MAX_VALUE);
    final var versionCandidateId = String.valueOf(versionCandidateNumber);
    final var bpmnPath = "bpmn";

    final var changeInfo = initChangeInfo(versionCandidateNumber);
    final var changeInfoDto = initChangeInfoDto(changeInfo);

    final var versionCandidateCloneResult = jGitWrapperMock.mockCloneCommand(versionCandidateId);
    gerritApiMock.mockGetChangeInfo(versionCandidateId, changeInfo);
    final var openRepoResult = jGitWrapperMock.mockOpenGit(versionCandidateId);
    final var fetchCommand = jGitWrapperMock.mockFetchCommand(openRepoResult, changeInfoDto);
    final var checkoutCommand = jGitWrapperMock.mockCheckoutCommand(openRepoResult);
    jGitWrapperMock.mockGetFileList(openRepoResult, bpmnPath, List.of());

    mockMvc.perform(get(BASE_REQUEST, versionCandidateId)
        .accept(MediaType.APPLICATION_JSON_VALUE)).andExpectAll(
        status().isOk(),
        content().contentType("application/json"),
        jsonPath("$", hasSize(0))
    );

    Mockito.verify(versionCandidateCloneResult).close();
    Mockito.verify(fetchCommand).call();
    Mockito.verify(checkoutCommand).call();
  }

  @Test
  @SneakyThrows
  public void createBusinessProcess() {
    final var versionCandidateNumber = new Random().nextInt(Integer.MAX_VALUE);
    final var versionCandidateId = String.valueOf(versionCandidateNumber);
    final var bpName = RandomString.make();
    final var bpTitle = RandomString.make();
    final var bpmnPath = "bpmn";
    final var bpFileName = String.format("%s.bpmn", bpName);
    final var bpFileRelativePath = String.format("%s/%s", bpmnPath, bpFileName);
    final var commitId = RandomString.make();
    final var bpFileContent = String.format(testProcessFormat, bpName, bpTitle);

    InitialisationUtils.createTempRepo(versionCandidateId);
    final var bpFileFullPath = Path.of(tempRepoDirectory.getPath(), versionCandidateId, bpmnPath,
        bpFileName);
    Assertions.assertThat(bpFileFullPath.toFile().exists()).isFalse();

    final var changeInfo = initChangeInfo(versionCandidateNumber);
    final var changeInfoDto = initChangeInfoDto(changeInfo);

    gerritApiMock.mockGetChangeInfo(versionCandidateId, changeInfo);
    final var openRepoResult = jGitWrapperMock.mockOpenGit(versionCandidateId);
    jGitWrapperMock.mockGetFileList(openRepoResult, bpmnPath, List.of());
    final var fetchCommand = jGitWrapperMock.mockFetchCommand(openRepoResult, changeInfoDto);
    final var checkoutCommand = jGitWrapperMock.mockCheckoutCommand(openRepoResult);
    final var addCommand = jGitWrapperMock.mockAddCommand(openRepoResult, bpFileRelativePath);
    final var status = jGitWrapperMock.mockStatusCommand(openRepoResult, false);
    final var commitCommand = jGitWrapperMock.mockCommitCommand(openRepoResult, commitId,
        changeInfoDto);
    final var remoteAdd = jGitWrapperMock.mockRemoteAddCommand(openRepoResult);
    final var pushCommand = jGitWrapperMock.mockPushCommand(openRepoResult);
    jGitWrapperMock.mockGetFileContent(openRepoResult, bpFileRelativePath, bpFileContent);

    mockMvc.perform(MockMvcRequestBuilders.post(
            BASE_REQUEST + "/{businessProcessName}", versionCandidateId, bpName)
        .contentType(MediaType.TEXT_XML).content(bpFileContent)
        .accept(MediaType.TEXT_XML)).andExpectAll(
        status().isCreated(),
        content().contentType("text/xml"),
        xpath("/bpmn:definitions/bpmn:process/@id", BPMN_NAMESPACES).string(bpName),
        xpath("/bpmn:definitions/bpmn:process/@name", BPMN_NAMESPACES).string(bpTitle));

    Mockito.verify(fetchCommand, Mockito.times(2)).call();
    Mockito.verify(checkoutCommand, Mockito.times(2)).call();
    Mockito.verify(addCommand).call();
    Mockito.verify(status).isClean();
    Mockito.verify(commitCommand).call();
    Mockito.verify(remoteAdd).call();
    Mockito.verify(pushCommand).call();
    Mockito.verify(pushCommand)
        .setRefSpecs(new RefSpec("HEAD:refs/for/" + gerritPropertiesConfig.getHeadBranch()));

    Assertions.assertThat(bpFileFullPath.toFile().exists()).isTrue();

    final var actualContent = Files.readString(bpFileFullPath);
    assertNoDifferences(bpFileContent, actualContent);

    final var document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(new InputSource(new StringReader(actualContent)));
    final var createdXpath = XPathFactory.newInstance().newXPath();
    final var created = createdXpath.compile("/definitions/@created").evaluate(document);
    final var updated = createdXpath.compile("/definitions/@modified").evaluate(document);
    Assertions.assertThat(LocalDateTime.parse(created, JacksonConfig.DATE_TIME_FORMATTER))
        .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
    Assertions.assertThat(LocalDateTime.parse(updated, JacksonConfig.DATE_TIME_FORMATTER))
        .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
  }

  @Test
  @SneakyThrows
  public void updateBusinessProcess() {
    final var versionCandidateNumber = new Random().nextInt(Integer.MAX_VALUE);
    final var versionCandidateId = String.valueOf(versionCandidateNumber);
    final var bpName = RandomString.make();
    final var bpTitle = RandomString.make();
    final var bpmnPath = "bpmn";
    final var bpFileName = String.format("%s.bpmn", bpName);
    final var bpFileRelativePath = String.format("%s/%s", bpmnPath, bpFileName);
    final var commitId = RandomString.make();
    final var bpFileContent = String.format(testProcessFormat, bpName, bpTitle);
    final var createdDate = LocalDateTime.of(2022, 10, 20, 15, 12);
    final var updatedDate = LocalDateTime.of(2022, 10, 20, 18, 25);

    InitialisationUtils.createTempRepo(versionCandidateId);
    InitialisationUtils.createProcessXml(String.format(testProcessFormat, bpName, "oldTitle"),
        versionCandidateId, bpName);
    final var bpFileFullPath = Path.of(tempRepoDirectory.getPath(), versionCandidateId, bpmnPath,
        bpFileName);
    Assertions.assertThat(bpFileFullPath.toFile().exists()).isTrue();

    final var changeInfo = initChangeInfo(versionCandidateNumber);
    final var changeInfoDto = initChangeInfoDto(changeInfo);

    gerritApiMock.mockGetChangeInfo(versionCandidateId, changeInfo);
    final var openRepoResult = jGitWrapperMock.mockOpenGit(versionCandidateId);
    jGitWrapperMock.mockGetFileList(openRepoResult, bpmnPath, List.of(bpFileName));
    jGitWrapperMock.mockGitFileDates(openRepoResult, bpFileRelativePath, createdDate, updatedDate);
    final var fetchCommand = jGitWrapperMock.mockFetchCommand(openRepoResult, changeInfoDto);
    final var checkoutCommand = jGitWrapperMock.mockCheckoutCommand(openRepoResult);
    final var addCommand = jGitWrapperMock.mockAddCommand(openRepoResult, bpFileRelativePath);
    final var status = jGitWrapperMock.mockStatusCommand(openRepoResult, false);
    final var commitCommand = jGitWrapperMock.mockCommitCommand(openRepoResult, commitId,
        changeInfoDto);
    final var remoteAdd = jGitWrapperMock.mockRemoteAddCommand(openRepoResult);
    final var pushCommand = jGitWrapperMock.mockPushCommand(openRepoResult);
    jGitWrapperMock.mockGetFileContent(openRepoResult, bpFileRelativePath, bpFileContent);

    mockMvc.perform(MockMvcRequestBuilders.put(
            BASE_REQUEST + "/{businessProcessName}", versionCandidateId, bpName)
        .contentType(MediaType.TEXT_XML).content(bpFileContent)
        .accept(MediaType.TEXT_XML)).andExpectAll(
        status().isOk(),
        content().contentType("text/xml"),
        xpath("/bpmn:definitions/bpmn:process/@id", BPMN_NAMESPACES).string(bpName),
        xpath("/bpmn:definitions/bpmn:process/@name", BPMN_NAMESPACES).string(bpTitle));

    Mockito.verify(fetchCommand, Mockito.times(2)).call();
    Mockito.verify(checkoutCommand, Mockito.times(2)).call();
    Mockito.verify(addCommand).call();
    Mockito.verify(status).isClean();
    Mockito.verify(commitCommand).call();
    Mockito.verify(remoteAdd).call();
    Mockito.verify(pushCommand).call();
    Mockito.verify(pushCommand)
        .setRefSpecs(new RefSpec("HEAD:refs/for/" + gerritPropertiesConfig.getHeadBranch()));

    Assertions.assertThat(bpFileFullPath.toFile().exists()).isTrue();

    final var actualContent = Files.readString(bpFileFullPath);
    assertNoDifferences(bpFileContent, actualContent);

    final var document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(new InputSource(new StringReader(actualContent)));
    final var createdXpath = XPathFactory.newInstance().newXPath();
    final var created = createdXpath.compile("/definitions/@created").evaluate(document);
    final var updated = createdXpath.compile("/definitions/@modified").evaluate(document);
    Assertions.assertThat(LocalDateTime.parse(created, JacksonConfig.DATE_TIME_FORMATTER))
        .isEqualTo(createdDate);
    Assertions.assertThat(LocalDateTime.parse(updated, JacksonConfig.DATE_TIME_FORMATTER))
        .isNotEqualTo(updatedDate)
        .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
  }

  @Test
  @SneakyThrows
  public void deleteBusinessProcess() {
    final var versionCandidateNumber = new Random().nextInt(Integer.MAX_VALUE);
    final var versionCandidateId = String.valueOf(versionCandidateNumber);
    final var bpName = RandomString.make();
    final var bpmnPath = "bpmn";
    final var bpFileName = String.format("%s.bpmn", bpName);
    final var bpFileRelativePath = String.format("%s/%s", bpmnPath, bpFileName);
    final var commitId = RandomString.make();

    InitialisationUtils.createTempRepo(versionCandidateId);
    InitialisationUtils.createProcessXml(RandomString.make(), versionCandidateId, bpName);
    final var bpFileFullPath = Path.of(tempRepoDirectory.getPath(), versionCandidateId, bpmnPath,
        bpFileName);
    Assertions.assertThat(bpFileFullPath.toFile().exists()).isTrue();

    final var changeInfo = initChangeInfo(versionCandidateNumber);
    final var changeInfoDto = initChangeInfoDto(changeInfo);

    gerritApiMock.mockGetChangeInfo(versionCandidateId, changeInfo);
    final var openRepoResult = jGitWrapperMock.mockOpenGit(versionCandidateId);
    jGitWrapperMock.mockGetFileList(openRepoResult, bpmnPath, List.of(bpFileName));
    jGitWrapperMock.mockGitFileDates(openRepoResult, bpFileRelativePath, LocalDateTime.now(),
        LocalDateTime.now());
    final var fetchCommand = jGitWrapperMock.mockFetchCommand(openRepoResult, changeInfoDto);
    final var checkoutCommand = jGitWrapperMock.mockCheckoutCommand(openRepoResult);
    final var rmCommand = jGitWrapperMock.mockRmCommand(openRepoResult, bpFileRelativePath);
    final var status = jGitWrapperMock.mockStatusCommand(openRepoResult, false);
    final var commitCommand = jGitWrapperMock.mockCommitCommand(openRepoResult, commitId,
        changeInfoDto);
    final var remoteAdd = jGitWrapperMock.mockRemoteAddCommand(openRepoResult);
    final var pushCommand = jGitWrapperMock.mockPushCommand(openRepoResult);

    mockMvc.perform(MockMvcRequestBuilders.delete(
            BASE_REQUEST + "/{businessProcessName}", versionCandidateId, bpName))
        .andExpect(status().isNoContent());

    Mockito.verify(fetchCommand, Mockito.times(2)).call();
    Mockito.verify(checkoutCommand, Mockito.times(2)).call();
    Mockito.verify(rmCommand).call();
    Mockito.verify(status).isClean();
    Mockito.verify(commitCommand).call();
    Mockito.verify(remoteAdd).call();
    Mockito.verify(pushCommand).call();
    Mockito.verify(pushCommand)
        .setRefSpecs(new RefSpec("HEAD:refs/for/" + gerritPropertiesConfig.getHeadBranch()));

    Assertions.assertThat(bpFileFullPath.toFile().exists()).isFalse();
  }

  private static void assertNoDifferences(String bpFileContent, String actualContent) {
    final var documentDiff = DiffBuilder
        .compare(actualContent)
        .withTest(bpFileContent)
        .withAttributeFilter(
            attr -> !attr.getName().equals("rrm:modified") && !attr.getName().equals("rrm:created"))
        .build();
    Assertions.assertThat(documentDiff.hasDifferences()).isFalse();
  }
}
