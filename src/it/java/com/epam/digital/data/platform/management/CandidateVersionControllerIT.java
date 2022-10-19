/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import static com.epam.digital.data.platform.management.util.InitialisationUtils.initFormDetails;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.epam.digital.data.platform.management.model.dto.CreateVersionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.FileInfo;
import com.google.gerrit.extensions.common.RevisionInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

public class CandidateVersionControllerIT extends BaseIT {

  private static final String BASE_REQUEST = "/versions/candidates/";

  @Test
  @SneakyThrows
  public void getVersionsList() {
    var changesInfoList = new ArrayList<ChangeInfo>();
    changesInfoList.add(initChangeInfo(1, "admin", "admin@epam.com", "admin"));
    changesInfoList.add(initChangeInfo(2, "user", "user@epam.com", "user"));
    gerritApiMock.mockGetVersionsList(changesInfoList);
    mockMvc.perform(MockMvcRequestBuilders.get("/versions/candidates")
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpectAll(
            status().isOk(),
            content().contentType("application/json"),
            jsonPath("$", hasSize(2)),
            jsonPath("$[0].id", is("1"))
        );
  }

  @Test
  @SneakyThrows
  public void getVersionsList_noVersions() {
    var changesInfoList = new ArrayList<ChangeInfo>();
    gerritApiMock.mockGetVersionsList(changesInfoList);
    mockMvc.perform(MockMvcRequestBuilders.get(BASE_REQUEST)
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpectAll(
            status().isOk(),
            jsonPath("$", hasSize(0))
        );
  }

  @Test
  @SneakyThrows
  public void getVersionDetails() {
    String candidateId = "candidateId";
    gerritApiMock.mockGetMRByNumber(candidateId,
        initChangeInfo(1, "admin", "admin@epam.com", "admin"));
    mockMvc.perform(MockMvcRequestBuilders.get(BASE_REQUEST + candidateId)
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpectAll(
            status().isOk(),
            content().contentType("application/json"),
            jsonPath("$.id", is("1")),
            jsonPath("$.author", is("admin")),
            jsonPath("$.description", is("this is description for version candidate 1")),
            jsonPath("$.name", is("commit message")),
            jsonPath("$.validations", nullValue())
        );
  }

  @Test
  @SneakyThrows
  public void createNewVersion() {
    ObjectMapper mapper = new ObjectMapper();
    CreateVersionRequest subject = new CreateVersionRequest();
    ChangeInfo changeInfo = initChangeInfo(1, "admin", "admin@epam.com", "admin");
    subject.setDescription("request description");
    subject.setName("request name");
    gerritApiMock.mockGetMRByNumber(String.valueOf(changeInfo._number), changeInfo);
    gerritApiMock.mockCreateChanges(changeInfo, subject);
    mockMvc.perform(
            MockMvcRequestBuilders.post(BASE_REQUEST).contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(mapper.writeValueAsString(subject))
                .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpectAll(
            status().isCreated(),
            content().contentType("application/json"),
            jsonPath("$.id", is("1")),
            jsonPath("$.author", is("admin")),
            jsonPath("$.description", is("this is description for version candidate 1")),
            jsonPath("$.name", is("commit message")),
            jsonPath("$.validations", nullValue())
        );
  }

  @Test
  @SneakyThrows
  public void getVersionDetails_noSuchVersion() {
    String candidateId = "id1";
    gerritApiMock.mockGetMRByNumber(candidateId, null);
    mockMvc.perform(MockMvcRequestBuilders.get(BASE_REQUEST + candidateId)
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpectAll(status().isInternalServerError());
  }

  @Test
  @SneakyThrows
  public void submitVersion() {
    String candidateId = "id1";
    gerritApiMock.mockGetMRByNumber(candidateId,
        initChangeInfo(1, "admin", "admin@epam.com", "admin"));
    gerritApiMock.mockSubmit();
    mockMvc.perform(MockMvcRequestBuilders.post(BASE_REQUEST + candidateId + "/submit")
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpectAll(status().isOk());
  }

  @Test
  @SneakyThrows
  public void declineCandidate() {
    String candidateId = "id1";
    gerritApiMock.mockGetMRByNumber(candidateId,
        initChangeInfo(1, "admin", "admin@epam.com", "admin"));
    mockMvc.perform(MockMvcRequestBuilders.post(BASE_REQUEST + candidateId + "/decline")
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpectAll(status().isOk());
  }

  @Test
  @SneakyThrows
  public void declineCandidate_NoSuchVersion() {
    String candidateId = "noSuchVersion";
    gerritApiMock.mockGetMRByNumber(candidateId,
        initChangeInfo(1, "admin", "admin@epam.com", "admin"));
    gerritApiMock.mockNotFound(candidateId);
    mockMvc.perform(MockMvcRequestBuilders.post(BASE_REQUEST + candidateId + "/decline")
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpectAll(status().isNotFound());
  }

  @Test
  @SneakyThrows
  public void submitVersion_NoSuchVersion() {
    String candidateId = "noSuchVersion";
    gerritApiMock.mockGetMRByNumber(candidateId,
        initChangeInfo(1, "admin", "admin@epam.com", "admin"));
    gerritApiMock.mockNotFound(candidateId);
    mockMvc.perform(MockMvcRequestBuilders.post(BASE_REQUEST + candidateId + "/submit")
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpectAll(status().isNotFound());
  }

  @Test
  @SneakyThrows
  public void getVersionChanges_noChanges() {
    String versionCandidateId = "id1";
    String formName = "formName";
    ChangeInfo changeInfo = initChangeInfo(1, "admin", "admin@epam.com", "admin");
    ChangeInfoDto changeInfoDto = initChangeInfoDto(versionCandidateId);
    var formDetails = initFormDetails(formName, "title",
        "{\"name\":\"" + formName + "\", \"title\":\"title\"}");
    FileInfo fileInfo = new FileInfo();
    fileInfo.status = 'A';
    changeInfo.revisions = new HashMap<>();
    RevisionInfo revisionInfo = new RevisionInfo();
    revisionInfo.ref = versionCandidateId;
    changeInfo.revisions.put(formName, revisionInfo);
    changeInfo.currentRevision = formName;
    changeInfoDto.setRefs(versionCandidateId);
    jGitWrapperMock.mockCloneCommand(versionCandidateId);
    jGitWrapperMock.mockGetForm(formDetails);
    jGitWrapperMock.mockCheckoutCommand();
    jGitWrapperMock.mockFetchCommand(changeInfoDto);
    jGitWrapperMock.mockPullCommand();
    gerritApiMock.mockGetMRByNumber(versionCandidateId, changeInfo);
    revisionInfo.ref = "id1";
    changeInfo.currentRevision = versionCandidateId;
    changeInfo.revisions = new HashMap<>();
    changeInfo.revisions.put(versionCandidateId, revisionInfo);
    jGitWrapperMock.mockGetFormsList(List.of(formDetails));
    jGitWrapperMock.mockLogCommand();
    mockMvc.perform(MockMvcRequestBuilders.get(BASE_REQUEST + versionCandidateId + "/changes")
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpectAll(
            status().isOk(),
            content().contentType("application/json"),
            jsonPath("$.changedForms", hasSize(0)),
            jsonPath("changedBusinessProcesses", hasSize(0)));
  }

  @Test
  @SneakyThrows
  public void getVersionChanges() {
    String versionCandidateId = "id1";
    String formName = "formName";
    ChangeInfo changeInfo = initChangeInfo(1, "admin", "admin@epam.com", "admin");
    ChangeInfoDto changeInfoDto = initChangeInfoDto(versionCandidateId);
    var formDetails = initFormDetails(formName, "title",
        "{\"name\":\"" + formName + "\", \"title\":\"title\"}");
    FileInfo fileInfo = new FileInfo();
    fileInfo.status = 'A';
    changeInfo.revisions = new HashMap<>();
    RevisionInfo revisionInfo = new RevisionInfo();
    revisionInfo.ref = versionCandidateId;
    changeInfo.revisions.put(formName, revisionInfo);
    changeInfo.currentRevision = formName;
    changeInfoDto.setRefs(versionCandidateId);
    jGitWrapperMock.mockCloneCommand(versionCandidateId);
    jGitWrapperMock.mockGetForm(formDetails);
    jGitWrapperMock.mockCheckoutCommand();
    jGitWrapperMock.mockFetchCommand(changeInfoDto);
    jGitWrapperMock.mockPullCommand();
    gerritApiMock.mockGetMRByNumber(versionCandidateId, changeInfo);
    revisionInfo.ref = "id1";
    changeInfo.currentRevision = versionCandidateId;
    changeInfo.revisions = new HashMap<>();
    changeInfo.revisions.put(versionCandidateId, revisionInfo);
    jGitWrapperMock.mockGetFormsList(List.of(formDetails));
    jGitWrapperMock.mockLogCommand();
    gerritApiMock.mockGetChangesInMr(Map.of("forms/formName.json", fileInfo));
    mockMvc.perform(MockMvcRequestBuilders.get(BASE_REQUEST + versionCandidateId + "/changes")
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpectAll(
            status().isOk(),
            content().contentType("application/json"),
            jsonPath("$.changedForms", hasSize(1)),
            jsonPath("changedBusinessProcesses", hasSize(0)));
  }
}
