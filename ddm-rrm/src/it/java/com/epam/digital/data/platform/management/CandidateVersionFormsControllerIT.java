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

import static com.epam.digital.data.platform.management.util.InitialisationUtils.createFormJson;
import static com.epam.digital.data.platform.management.util.InitialisationUtils.deleteFormJson;
import static com.epam.digital.data.platform.management.util.InitialisationUtils.initChangeInfo;
import static com.epam.digital.data.platform.management.util.InitialisationUtils.initChangeInfoDto;
import static com.epam.digital.data.platform.management.util.InitialisationUtils.initFormDetails;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.core.config.JacksonConfig;
import com.epam.digital.data.platform.management.dto.TestFormDetailsShort;
import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.epam.digital.data.platform.management.util.InitialisationUtils;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.RevisionInfo;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.eclipse.jgit.transport.RefSpec;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

public class CandidateVersionFormsControllerIT extends BaseIT {

  private static final String BASE_REQUEST = "/versions/candidates/{versionCandidateId}/forms";
  private final Gson gson = new Gson();

  @Test
  @SneakyThrows
  public void getForm() {
    final var versionCandidateId = RandomString.make();
    String formName = "formName";
    ChangeInfo changeInfo = initChangeInfo(1, "admin", "admin@epam.com", "admin");
    ChangeInfoDto changeInfoDto = initChangeInfoDto(versionCandidateId);
    var formDetails = initFormDetails(formName, "title",
        "{\"name\":\"" + formName + "\", \"title\":\"title\"}");

    changeInfo.revisions = new HashMap<>();
    RevisionInfo revisionInfo = new RevisionInfo();
    revisionInfo.ref = versionCandidateId;
    changeInfo.revisions.put(formName, revisionInfo);
    changeInfo.currentRevision = formName;
    changeInfoDto.setRefs(versionCandidateId);

    final var versionCandidateCloneResult = jGitWrapperMock.mockCloneCommand(versionCandidateId);
    jGitWrapperMock.mockGetForm(formDetails);
    jGitWrapperMock.mockCheckoutCommand();
    jGitWrapperMock.mockPullCommand();
    jGitWrapperMock.mockFetchCommand(changeInfoDto);
    gerritApiMock.mockGetChangeInfo(versionCandidateId, changeInfo);
    mockMvc.perform(
        MockMvcRequestBuilders.get(BASE_REQUEST + "/{formName}", versionCandidateId, formName)
            .accept(MediaType.APPLICATION_JSON_VALUE)).andExpectAll(
        status().isOk(),
        content().contentType("application/json"),
        jsonPath("$.name", is(formDetails.getName())),
        jsonPath("$.title", is(formDetails.getTitle()))
    );

    Mockito.verify(versionCandidateCloneResult).close();
  }

  @Test
  @SneakyThrows
  public void getFormsByVersionId() {
    final var versionCandidateId = RandomString.make();
    String formName = "formName";
    ChangeInfo changeInfo = initChangeInfo(1, "admin", "admin@epam.com", "admin");
    ChangeInfoDto changeInfoDto = initChangeInfoDto(versionCandidateId);

    changeInfo.revisions = new HashMap<>();
    RevisionInfo revisionInfo = new RevisionInfo();
    revisionInfo.ref = "id1";
    changeInfo.revisions.put(formName, revisionInfo);
    changeInfo.currentRevision = formName;
    changeInfoDto.setRefs(versionCandidateId);
    var list = new ArrayList<TestFormDetailsShort>();
    list.add(initFormDetails("name", "title", "{\"name\":\"name\", \"title\":\"title\"}"));
    list.add(initFormDetails("name2", "title2", "{\"name\":\"name2\", \"title\":\"title2\"}"));

    final var versionCandidateCloneResult = jGitWrapperMock.mockCloneCommand(versionCandidateId);
    jGitWrapperMock.mockGetFormsList(list);
    jGitWrapperMock.mockLogCommand();
    jGitWrapperMock.mockCheckoutCommand();
    jGitWrapperMock.mockPullCommand();
    jGitWrapperMock.mockFetchCommand(changeInfoDto);
    jGitWrapperMock.mockCheckoutCommand();
    jGitWrapperMock.mockPullCommand();
    jGitWrapperMock.mockFetchCommand(changeInfoDto);
    gerritApiMock.mockGetChangeInfo(versionCandidateId, changeInfo);
    mockMvc.perform(MockMvcRequestBuilders.get(BASE_REQUEST, versionCandidateId)
        .accept(MediaType.APPLICATION_JSON_VALUE)).andExpectAll(
        status().isOk(),
        content().contentType("application/json"),
        jsonPath("$[0].name", is("name")),
        jsonPath("$[0].title", is("title")),
        jsonPath("$[1].name", is("name2")),
        jsonPath("$[1].title", is("title2"))
    );

    Mockito.verify(versionCandidateCloneResult).close();
  }

  @Test
  @SneakyThrows
  public void getFormsByVersionIdNoForms() {
    String versionCandidateId = "-1";
    String formName = "formName";
    ChangeInfo changeInfo = initChangeInfo(1, "admin", "admin@epam.com", "admin");
    ChangeInfoDto changeInfoDto = initChangeInfoDto(versionCandidateId);

    changeInfo.revisions = new HashMap<>();
    RevisionInfo revisionInfo = new RevisionInfo();
    revisionInfo.ref = "-1";
    changeInfo.revisions.put(formName, revisionInfo);
    changeInfo.currentRevision = formName;
    changeInfoDto.setRefs(versionCandidateId);

    final var versionCandidateCloneResult = jGitWrapperMock.mockCloneCommand(versionCandidateId);
    jGitWrapperMock.mockGetFormsList(new ArrayList<>());
    jGitWrapperMock.mockCheckoutCommand();
    jGitWrapperMock.mockPullCommand();
    jGitWrapperMock.mockFetchCommand(changeInfoDto);
    gerritApiMock.mockGetChangeInfo(versionCandidateId, changeInfo);
    mockMvc.perform(MockMvcRequestBuilders.get(BASE_REQUEST, versionCandidateId)
        .accept(MediaType.APPLICATION_JSON_VALUE)).andExpectAll(
        status().isOk(),
        content().contentType("application/json"),
        jsonPath("$", hasSize(0))
    );

    Mockito.verify(versionCandidateCloneResult).close();
  }

  @Test
  @SneakyThrows
  public void createForm() {
    final var versionCandidateNumber = new Random().nextInt(Integer.MAX_VALUE);
    final var versionCandidateId = String.valueOf(versionCandidateNumber);
    final var formName = RandomString.make();
    final var formPath = "forms";
    final var formFileName = String.format("%s.json", formName);
    final var formFileRelativePath = String.format("%s/%s", formPath, formFileName);
    final var commitId = RandomString.make();
    final var formContent = testForm;

    InitialisationUtils.createTempRepo(versionCandidateId);
    final var formFileFullPath = Path.of(tempRepoDirectory.getPath(), versionCandidateId, formPath,
        formFileName);
    Assertions.assertThat(formFileFullPath.toFile().exists()).isFalse();

    final var changeInfo = initChangeInfo(versionCandidateNumber);
    final var changeInfoDto = initChangeInfoDto(changeInfo);

    gerritApiMock.mockGetChangeInfo(versionCandidateId, changeInfo);
    final var openRepoResult = jGitWrapperMock.mockOpenGit(versionCandidateId);
    jGitWrapperMock.mockGetFileList(openRepoResult, formPath, List.of());
    final var fetchCommand = jGitWrapperMock.mockFetchCommand(openRepoResult, changeInfoDto);
    final var checkoutCommand = jGitWrapperMock.mockCheckoutCommand(openRepoResult);
    final var addCommand = jGitWrapperMock.mockAddCommand(openRepoResult, formFileRelativePath);
    final var status = jGitWrapperMock.mockStatusCommand(openRepoResult, false);
    final var commitCommand = jGitWrapperMock.mockCommitCommand(openRepoResult, commitId,
        changeInfoDto);
    final var remoteAdd = jGitWrapperMock.mockRemoteAddCommand(openRepoResult);
    final var pushCommand = jGitWrapperMock.mockPushCommand(openRepoResult);
    jGitWrapperMock.mockGetFileContent(openRepoResult, formFileRelativePath, formContent);

    mockMvc.perform(MockMvcRequestBuilders.post(
            BASE_REQUEST + "/{formName}", versionCandidateId, formName)
        .contentType(MediaType.TEXT_XML).content(formContent)
        .accept(MediaType.APPLICATION_JSON)).andExpectAll(
        status().isCreated(),
        content().contentType("application/json"),
        jsonPath("$.title", is("Update physical factors")),
        jsonPath("$.path", is("add-fizfactors1")),
        jsonPath("$.display", is("form")),
        jsonPath("$.components", hasSize(0)),
        jsonPath("$.name", is("add-fizfactors1"))
    );

    Mockito.verify(fetchCommand, Mockito.times(2)).call();
    Mockito.verify(checkoutCommand, Mockito.times(2)).call();
    Mockito.verify(addCommand).call();
    Mockito.verify(status).isClean();
    Mockito.verify(commitCommand).call();
    Mockito.verify(remoteAdd).call();
    Mockito.verify(pushCommand).call();
    Mockito.verify(pushCommand)
        .setRefSpecs(new RefSpec("HEAD:refs/for/" + gerritPropertiesConfig.getHeadBranch()));

    Assertions.assertThat(formFileFullPath.toFile().exists()).isTrue();

    final var actualContent = Files.readString(formFileFullPath);
    JSONAssert.assertEquals(formContent, actualContent,
        new CustomComparator(JSONCompareMode.LENIENT,
            new Customization("created", (o1, o2) -> true),
            new Customization("modified", (o1, o2) -> true)
        ));

    var form = JsonParser.parseString(actualContent).getAsJsonObject();

    final var created = LocalDateTime.parse(form.get("created").getAsString(), JacksonConfig.DATE_TIME_FORMATTER).format(JacksonConfig.DATE_TIME_FORMATTER);
    final var updated = LocalDateTime.parse(form.get("modified").getAsString(), JacksonConfig.DATE_TIME_FORMATTER).format(JacksonConfig.DATE_TIME_FORMATTER);
    Assertions.assertThat(LocalDateTime.parse(created, JacksonConfig.DATE_TIME_FORMATTER))
        .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
    Assertions.assertThat(LocalDateTime.parse(updated, JacksonConfig.DATE_TIME_FORMATTER))
        .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
  }

  @Test
  @SneakyThrows
  public void updateForm() {
    final var versionCandidateId = RandomString.make();
    String formName = "formName";
    ChangeInfo changeInfo = initChangeInfo(1, "admin", "admin@epam.com", "admin");
    ChangeInfoDto changeInfoDto = initChangeInfoDto(versionCandidateId);
    var form = initFormDetails(formName, "title",
        "{\"name\":\"" + formName + "\", \"title\":\"title\"}");

    changeInfo.revisions = new HashMap<>();
    RevisionInfo revisionInfo = new RevisionInfo();
    revisionInfo.ref = "id1";
    changeInfo.revisions.put(formName, revisionInfo);
    changeInfo.revisions.put(versionCandidateId, revisionInfo);
    changeInfo.currentRevision = versionCandidateId;
    changeInfoDto.setRefs(versionCandidateId);
    gerritApiMock.mockGetChangeInfo(versionCandidateId, changeInfo);
    jGitWrapperMock.mockCheckoutCommand();
    jGitWrapperMock.mockPullCommand();
    InitialisationUtils.createTempRepo(versionCandidateId);
    String filePath = createFormJson(form, versionCandidateId);
    jGitWrapperMock.mockFetchCommand(changeInfoDto);
    jGitWrapperMock.mockAddCommand();
    jGitWrapperMock.mockStatusCommand();
    jGitWrapperMock.mockRemoteAddCommand();
    jGitWrapperMock.mockPushCommand();
    jGitWrapperMock.mockCommitCommand();
    jGitWrapperMock.mockLogCommand();
    jGitWrapperMock.mockGetForm(form);
    jGitWrapperMock.mockGetFormsList(List.of(form));
    mockMvc.perform(MockMvcRequestBuilders.put(
            BASE_REQUEST + "/{formName}", versionCandidateId, formName)
        .contentType(MediaType.APPLICATION_JSON_VALUE).content(gson.toJson(form))
        .accept(MediaType.APPLICATION_JSON_VALUE)).andExpectAll(
        status().isOk(),
        content().contentType("application/json"));
    deleteFormJson(filePath);
  }

  @Test
  @SneakyThrows
  public void deleteForm() {
    final var versionCandidateNumber = new Random().nextInt(Integer.MAX_VALUE);
    final var versionCandidateId = String.valueOf(versionCandidateNumber);
    final var formName = RandomString.make();
    final var formPath = "forms";
    final var formFileName = String.format("%s.json", formName);
    final var formFileRelativePath = String.format("%s/%s", formPath, formFileName);
    final var commitId = RandomString.make();
    var form = initFormDetails(formName, "title",
        "{\"name\":\"" + formName + "\", \"title\":\"title\"}");
    InitialisationUtils.createTempRepo(versionCandidateId);
    InitialisationUtils.createFormJson(form, versionCandidateId);
    final var formFileFullPath = Path.of(tempRepoDirectory.getPath(), versionCandidateId, formPath,
        formFileName);
    Assertions.assertThat(formFileFullPath.toFile().exists()).isTrue();

    final var changeInfo = initChangeInfo(versionCandidateNumber);
    final var changeInfoDto = initChangeInfoDto(changeInfo);

    gerritApiMock.mockGetChangeInfo(versionCandidateId, changeInfo);
    final var openRepoResult = jGitWrapperMock.mockOpenGit(versionCandidateId);
    jGitWrapperMock.mockGetFileList(openRepoResult, formPath, List.of(formFileName));
    jGitWrapperMock.mockGitFileDates(openRepoResult, formFileRelativePath, LocalDateTime.now(),
        LocalDateTime.now());
    final var fetchCommand = jGitWrapperMock.mockFetchCommand(openRepoResult, changeInfoDto);
    final var checkoutCommand = jGitWrapperMock.mockCheckoutCommand(openRepoResult);
    final var rmCommand = jGitWrapperMock.mockRmCommand(openRepoResult, formFileRelativePath);
    final var status = jGitWrapperMock.mockStatusCommand(openRepoResult, false);
    final var commitCommand = jGitWrapperMock.mockCommitCommand(openRepoResult, commitId,
        changeInfoDto);
    final var remoteAdd = jGitWrapperMock.mockRemoteAddCommand(openRepoResult);
    final var pushCommand = jGitWrapperMock.mockPushCommand(openRepoResult);

    mockMvc.perform(MockMvcRequestBuilders.delete(
            BASE_REQUEST + "/{businessProcessName}", versionCandidateId, formName))
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
  }
}
