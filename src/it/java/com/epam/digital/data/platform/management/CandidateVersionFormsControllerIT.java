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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.dto.TestFormDetailsShort;
import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.epam.digital.data.platform.management.util.InitialisationUtils;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.RevisionInfo;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.SneakyThrows;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
    gerritApiMock.mockGetMRByNumber(versionCandidateId, changeInfo);
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
    gerritApiMock.mockGetMRByNumber(versionCandidateId, changeInfo);
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
    gerritApiMock.mockGetMRByNumber(versionCandidateId, changeInfo);
    mockMvc.perform(MockMvcRequestBuilders.get(BASE_REQUEST, versionCandidateId)
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
  public void createForm() {
    //todo fix this test
    String versionCandidateId = "id1";
    Gson gson = new Gson();
    String formName = "formName";
    ChangeInfo changeInfo = initChangeInfo(1, "admin", "admin@epam.com", "admin");
    ChangeInfoDto changeInfoDto = initChangeInfoDto(versionCandidateId);
    changeInfo.revisions = new HashMap<>();
    RevisionInfo revisionInfo = new RevisionInfo();
    revisionInfo.ref = "id1";
    changeInfo.revisions.put(formName, revisionInfo);
    changeInfo.currentRevision = formName;
    changeInfoDto.setRefs(versionCandidateId);
    var form = initFormDetails(formName, "title",
        "{\"name\":\"" + formName + "\", \"title\":\"title\"}");
    final var versionCandidateCloneResult = jGitWrapperMock.mockCloneCommand(versionCandidateId);
    jGitWrapperMock.mockCheckoutCommand();
    jGitWrapperMock.mockPullCommand();
    jGitWrapperMock.mockFetchCommand(changeInfoDto);
    jGitWrapperMock.mockGetForm(form);
    jGitWrapperMock.mockLogCommand();
    jGitWrapperMock.mockGetFormsList(List.of(form));
    jGitWrapperMock.mockAddCommand();
    jGitWrapperMock.mockStatusCommand();
    jGitWrapperMock.mockRemoteAddCommand();
    jGitWrapperMock.mockPushCommand();
    jGitWrapperMock.mockCommitCommand();
    gerritApiMock.mockGetMRByNumber(versionCandidateId, changeInfo);
    mockMvc.perform(MockMvcRequestBuilders.post(
            BASE_REQUEST + "/{formName}", versionCandidateId, formName)
        .contentType(MediaType.APPLICATION_JSON_VALUE).content(gson.toJson(form))
        .accept(MediaType.APPLICATION_JSON_VALUE)).andExpectAll(
        status().isCreated(),
        content().contentType("application/json"),
        jsonPath("$.name", is("formName")),
        jsonPath("$.title", is("title"))
    );

    Mockito.verify(versionCandidateCloneResult).close();
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
    gerritApiMock.mockGetMRByNumber(versionCandidateId, changeInfo);
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
    final var versionCandidateId = RandomString.make();
    String formName = "formName";
    ChangeInfo changeInfo = initChangeInfo(1, "admin", "admin@epam.com", "admin");
    ChangeInfoDto changeInfoDto = initChangeInfoDto(versionCandidateId);
    changeInfo.revisions = new HashMap<>();
    RevisionInfo revisionInfo = new RevisionInfo();
    revisionInfo.ref = "-1";
    changeInfo.revisions.put(formName, revisionInfo);
    changeInfo.currentRevision = formName;
    changeInfoDto.setRefs(revisionInfo.ref);
    gerritApiMock.mockGetMRByNumber(versionCandidateId, changeInfo);
    final var versionCandidateCloneResult = jGitWrapperMock.mockCloneCommand(versionCandidateId);
    jGitWrapperMock.mockGetFormsList(List.of(initFormDetails(formName, "title",
        "{\"name\":\"" + formName + "\", \"title\":\"title\"}")));
    jGitWrapperMock.mockLogCommand();
    jGitWrapperMock.mockCheckoutCommand();
    jGitWrapperMock.mockPullCommand();
    jGitWrapperMock.mockFetchCommand(changeInfoDto);
    mockMvc.perform(MockMvcRequestBuilders.delete(
            BASE_REQUEST + "/{formName}", versionCandidateId, formName))
        .andExpect(status().isNoContent());

    Mockito.verify(versionCandidateCloneResult).close();
  }
}
