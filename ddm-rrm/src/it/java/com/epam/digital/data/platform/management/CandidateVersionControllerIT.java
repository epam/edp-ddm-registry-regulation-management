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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.dto.TestVersionCandidate;
import com.epam.digital.data.platform.management.model.dto.CreateVersionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;


@DisplayName("Version candidate controller tests")
class CandidateVersionControllerIT extends BaseIT {

  @Nested
  @DisplayName("GET /versions/candidates")
  class CandidateVersionControllerVersionListIT {

    @Test
    @DisplayName("should return all of the versions that are found")
    @SneakyThrows
    void getVersionsList() {
      final var gerritProps = context.getGerritProps();
      final var versionCandidate1 = Map.of(
          "_number", 1,
          "subject", "John Doe's version candidate with number 1",
          "topic", "Implemented new feature",
          "change_id", "change_id1",
          "owner", Map.of("username", gerritProps.getUser()),
          "labels", Map.of()
      );
      final var versionCandidate2 = Map.of(
          "_number", 2,
          "subject", "John Doe's version candidate with number 2",
          "topic", "Bug fix",
          "change_id", "change_id2",
          "owner", Map.of("username", gerritProps.getUser()),
          "labels", Map.of()
      );
      final var versionsResponse = List.of(versionCandidate1, versionCandidate2);
      final var objectMapper = new ObjectMapper();
      context.getGerritMockServer().addStubMapping(stubFor(
          WireMock.get(String.format("/a/changes/?q=project:%s+status:open+owner:%s",
                  gerritProps.getRepository(), gerritProps.getUser()))
              .willReturn(aResponse().withStatus(200)
                  .withBody(objectMapper.writeValueAsString(versionsResponse)))));
      context.getGerritMockServer().addStubMapping(stubFor(
          WireMock.get(urlPathEqualTo("/a/changes/change_id1"))
              .willReturn(aResponse().withStatus(200)
                  .withBody(objectMapper.writeValueAsString(versionCandidate1)))));
      context.getGerritMockServer().addStubMapping(stubFor(
          WireMock.get(urlPathEqualTo("/a/changes/change_id1/revisions/current/mergeable"))
              .willReturn(aResponse().withStatus(200)
                  .withBody("{\"mergeable\":true}"))));
      context.getGerritMockServer().addStubMapping(stubFor(
          WireMock.get(urlPathEqualTo("/a/changes/change_id2"))
              .willReturn(aResponse().withStatus(200)
                  .withBody(objectMapper.writeValueAsString(versionCandidate2)))));
      context.getGerritMockServer().addStubMapping(stubFor(
          WireMock.get(urlPathEqualTo("/a/changes/change_id2/revisions/current/mergeable"))
              .willReturn(aResponse().withStatus(200)
                  .withBody("{\"mergeable\":false}"))));

      mockMvc.perform(get("/versions/candidates")
          .accept(MediaType.APPLICATION_JSON_VALUE)
      ).andExpectAll(
          status().isOk(),
          content().contentType("application/json"),
          jsonPath("$", hasSize(2)),
          jsonPath("$[0].id", is("1")),
          jsonPath("$[0].name", is("John Doe's version candidate with number 1")),
          jsonPath("$[0].description", is("Implemented new feature")),
          jsonPath("$[1].id", is("2")),
          jsonPath("$[1].name", is("John Doe's version candidate with number 2")),
          jsonPath("$[1].description", is("Bug fix"))
      );
    }

    @Test
    @DisplayName("should return empty list if no versions found")
    @SneakyThrows
    void getVersionsList_noVersions() {
      var gerritProps = context.getGerritProps();
      context.getGerritMockServer().addStubMapping(stubFor(WireMock.get(
              String.format("/a/changes/?q=project:%s+status:open+owner:%s",
                  gerritProps.getRepository(), gerritProps.getUser()))
          .willReturn(aResponse().withStatus(200).withBody("[]"))));

      mockMvc.perform(get("/versions/candidates")
          .accept(MediaType.APPLICATION_JSON_VALUE)
      ).andExpectAll(
          status().isOk(),
          jsonPath("$", hasSize(0))
      );
    }
  }

  @Nested
  @DisplayName("GET /versions/candidates/{versionCandidateId}")
  class CandidateVersionControllerVersionByIdIT {

    @Test
    @DisplayName("should return 200 if version exists")
    @SneakyThrows
    void getVersionDetails() {
      final var versionCandidateId = context.createVersionCandidate(
          TestVersionCandidate.builder()
              .subject("John Doe's useless version candidate")
              .topic("For fun")
              .created(LocalDateTime.of(2022, 10, 28, 15, 12))
              .updated(LocalDateTime.of(2022, 10, 29, 6, 47))
              .mergeable(false)
              .build()
      );

      mockMvc.perform(get("/versions/candidates/{versionCandidateId}", versionCandidateId)
          .accept(MediaType.APPLICATION_JSON_VALUE)
      ).andExpectAll(
          status().isOk(),
          content().contentType("application/json"),
          jsonPath("$.id", is(versionCandidateId)),
          jsonPath("$.name", is("John Doe's useless version candidate")),
          jsonPath("$.description", is("For fun")),
          jsonPath("$.hasConflicts", is(true)),
          jsonPath("$.creationDate", is("2022-10-28T15:12:00.000Z")),
          jsonPath("$.latestUpdate", is("2022-10-29T06:47:00.000Z")),
          jsonPath("$.author", is(context.getGerritProps().getUser())),
          jsonPath("$.validations", nullValue())
      );
    }

    @Test
    @DisplayName("should return 404 if version doesn't exist")
    @SneakyThrows
    void getVersionDetails_noSuchVersion() {
      final var versionCandidateId = context.mockVersionCandidateDoesNotExist();

      mockMvc.perform(get("/versions/candidates/{versionCandidateId}", versionCandidateId)
          .accept(MediaType.APPLICATION_JSON_VALUE)
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.code", is("CHANGE_NOT_FOUND")),
          jsonPath("$.details",
              is(String.format("Could not get change info for %s MR", versionCandidateId)))
      );
    }
  }

  @Nested
  @DisplayName("POST /versions/candidates/{versionCandidateId}/submit")
  class CandidateVersionControllerSubmitVersionIT {

    @Test
    @DisplayName("should return 200 if version exists and it's mergeable")
    @SneakyThrows
    void submitVersion() {
      // create version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // mock gerrit review change
      context.getGerritMockServer().addStubMapping(stubFor(
          WireMock.post(urlPathEqualTo(
                  String.format("/a/changes/%s/revisions/current/review", versionCandidateId)))
              .withRequestBody(matchingJsonPath("$.labels.Code-Review", equalTo("2")))
              .withRequestBody(matchingJsonPath("$.labels.Verified", equalTo("1")))
              .willReturn(aResponse().withStatus(200).withBody("{\"ready\":true}"))));
      // mock gerrit submit change
      context.getGerritMockServer().addStubMapping(stubFor(
          WireMock.post(urlPathEqualTo(String.format("/a/changes/%s/submit", versionCandidateId)))
              .willReturn(aResponse().withStatus(200).withBody("{}"))));

      // perform request
      mockMvc.perform(post("/versions/candidates/{versionCandidateId}/submit", versionCandidateId)
          .accept(MediaType.APPLICATION_JSON_VALUE)
      ).andExpectAll(status().isOk());
    }

    @Test
    @DisplayName("should return 404 if version doesn't exist")
    @SneakyThrows
    void submitVersion_NoSuchVersion() {
      final var versionCandidateId = context.mockVersionCandidateDoesNotExist();

      mockMvc.perform(post("/versions/candidates/{versionCandidateId}/submit", versionCandidateId)
          .accept(MediaType.APPLICATION_JSON_VALUE)
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.code", is("CHANGE_NOT_FOUND")),
          jsonPath("$.details",
              is(String.format("Could not found candidate with id %s", versionCandidateId)))
      );
    }
  }

  @Nested
  @DisplayName("POST /versions/candidates/{versionCandidateId}/decline")
  class CandidateVersionControllerDeclineVersionIT {

    @Test
    @DisplayName("should return 200 if version exists")
    @SneakyThrows
    void declineCandidate() {
      // create version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // mock gerrit abandon change
      context.getGerritMockServer().addStubMapping(stubFor(WireMock.post(
              urlPathEqualTo(String.format("/a/changes/%s/abandon", versionCandidateId)))
          .willReturn(aResponse().withStatus(200).withBody("{}"))));

      // perform request
      mockMvc.perform(post("/versions/candidates/{versionCandidateId}/decline", versionCandidateId)
          .accept(MediaType.APPLICATION_JSON_VALUE)
      ).andExpectAll(
          status().isOk()
      );
    }

    @Test
    @DisplayName("should return 404 if version doesn't exist")
    @SneakyThrows
    void declineCandidate_NoSuchVersion() {
      final var versionCandidateId = context.mockVersionCandidateDoesNotExist();

      mockMvc.perform(post("/versions/candidates/{versionCandidateId}/decline", versionCandidateId)
          .accept(MediaType.APPLICATION_JSON_VALUE)
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.code", is("CHANGE_NOT_FOUND")),
          jsonPath("$.details",
              is(String.format("Could not found candidate with id %s", versionCandidateId)))
      );
    }
  }

  @Nested
  @DisplayName("GET /versions/candidates/{versionCandidateId}/changes")
  class CandidateVersionControllerVersionChangesIT {

    @Test
    @DisplayName("should return 200 with all changes")
    @SneakyThrows
    void getVersionChanges() {
      // add forms to "remote" repo
      final var changedFormHead = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/changes/GET/changed-form-head.json");
      context.addFileToHeadRepo("/forms/changed-form.json", changedFormHead);
      final var unchangedForm = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/changes/GET/unchanged-form.json");
      context.addFileToHeadRepo("/forms/unchanged-form.json", unchangedForm);
      final var deletedForm = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/changes/GET/deleted-form.json");
      context.addFileToHeadRepo("/forms/deleted-form.json", deletedForm);

      // add business-processes to "remote" repo
      final var changedProcessHead = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/changes/GET/changed-process-head.bpmn");
      context.addFileToHeadRepo("/bpmn/changed-process.bpmn", changedProcessHead);
      final var unchangedProcess = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/changes/GET/unchanged-process.bpmn");
      context.addFileToHeadRepo("/bpmn/unchanged-process.bpmn", unchangedProcess);
      final var deletedProcess = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/changes/GET/deleted-process.bpmn");
      context.addFileToHeadRepo("/bpmn/deleted-process.bpmn", deletedProcess);

      // create version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // change forms in version candidate remote
      final var addedForm = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/changes/GET/added-form.json");
      context.addFileToVersionCandidateRemote("/forms/added-form.json", addedForm);
      final var changedFormVC = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/changes/GET/changed-form-version-candidate.json");
      context.addFileToVersionCandidateRemote("/forms/changed-form.json",
          changedFormVC);
      context.deleteFileFromVersionCandidateRemote("forms/deleted-form.json");

      // change business-processes in version candidate remote
      final var addedProcess = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/changes/GET/added-process.bpmn");
      context.addFileToVersionCandidateRemote("/bpmn/added-process.bpmn",
          addedProcess);
      final var changedProcessVC = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/changes/GET/changed-process-version-candidate.bpmn");
      context.addFileToVersionCandidateRemote("/bpmn/changed-process.bpmn",
          changedProcessVC);
      context.deleteFileFromVersionCandidateRemote("bpmn/deleted-process.bpmn");

      // mock gerrit response about changed files
      final var filesResponse = Map.of(
          "bpmn/added-process.bpmn", Map.of("status", "A"),
          "bpmn/changed-process.bpmn", Map.of("status", "R"),
          "bpmn/deleted-process.bpmn", Map.of("status", "D"),
          "forms/added-form.json", Map.of("status", "A"),
          "forms/changed-form.json", Map.of("status", "R"),
          "forms/deleted-form.jsom", Map.of("status", "D")
      );
      context.getGerritMockServer().addStubMapping(
          stubFor(WireMock.get(urlPathEqualTo(String.format("/a/changes/%s/revisions/current/files",
              context.getVersionCandidate().getChangeId()))
          ).willReturn(aResponse()
              .withStatus(200)
              .withBody(new ObjectMapper().writeValueAsString(filesResponse))))
      );

      // perform request
      mockMvc.perform(get("/versions/candidates/{versionCandidateId}/changes", versionCandidateId)
          .accept(MediaType.APPLICATION_JSON_VALUE)
      ).andExpectAll(
          status().isOk(),
          content().contentType("application/json"),
          jsonPath("$.changedForms", hasSize(3)),
          jsonPath("$.changedForms[0].name", is("added-form")),
          jsonPath("$.changedForms[0].title", is("Added form")),
          jsonPath("$.changedForms[0].status", is("NEW")),
          jsonPath("$.changedForms[1].name", is("changed-form")),
          jsonPath("$.changedForms[1].title", is("Changed form VC")),
          jsonPath("$.changedForms[1].status", is("CHANGED")),
          jsonPath("$.changedForms[2].name", is("deleted-form")),
          jsonPath("$.changedForms[2].title", is("Deleted form")),
          jsonPath("$.changedForms[2].status", is("DELETED")),
          jsonPath("$.changedBusinessProcesses", hasSize(3)),
          jsonPath("$.changedBusinessProcesses[0].name", is("added-process")),
          jsonPath("$.changedBusinessProcesses[0].title", is("Added process")),
          jsonPath("$.changedBusinessProcesses[0].status", is("NEW")),
          jsonPath("$.changedBusinessProcesses[1].name", is("changed-process")),
          jsonPath("$.changedBusinessProcesses[1].title", is("Changed process VC")),
          jsonPath("$.changedBusinessProcesses[1].status", is("CHANGED")),
          jsonPath("$.changedBusinessProcesses[2].name", is("deleted-process")),
          jsonPath("$.changedBusinessProcesses[2].title", is("Deleted process")),
          jsonPath("$.changedBusinessProcesses[2].status", is("DELETED"))
      );
    }

    @Test
    @DisplayName("should return 200 with empty lists if there are no changes")
    @SneakyThrows
    void getVersionChanges_noChanges() {
      // mock version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // perform request
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/changes", versionCandidateId)
              .accept(MediaType.APPLICATION_JSON_VALUE)
      ).andExpectAll(
          status().isOk(),
          content().contentType("application/json"),
          jsonPath("$.changedForms", hasSize(0)),
          jsonPath("$.changedBusinessProcesses", hasSize(0))
      );
    }

    @Test
    @DisplayName("should return 404 if version doesn't exist")
    @SneakyThrows
    void getVersionChanges_noSuchVersion() {
      final var versionCandidateId = context.mockVersionCandidateDoesNotExist();

      mockMvc.perform(get("/versions/candidates/{versionCandidateId}/changes", versionCandidateId)
          .accept(MediaType.APPLICATION_JSON_VALUE)
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.code", is("CHANGE_NOT_FOUND")),
          jsonPath("$.details", is(String.format("Could not get change info for %s MR", versionCandidateId)))
      );
    }
  }

  @Test
  @DisplayName("POST /versions/candidates should return 201 with version info")
  @SneakyThrows
  void createNewVersion() {
    final var versionCandidateId = context.createVersionCandidate(
        TestVersionCandidate.builder()
            .subject("request name")
            .topic("request description")
            .mergeable(true)
            .created(LocalDateTime.of(2022, 10, 28, 15, 34))
            .updated(LocalDateTime.of(2022, 10, 28, 15, 34))
            .build()
    );

    context.getGerritMockServer().addStubMapping(
        stubFor(WireMock.post("/a/changes/")
            .withRequestBody(matchingJsonPath("$.subject", equalTo("request name")))
            .withRequestBody(matchingJsonPath("$.topic", equalTo("request description")))
            .willReturn(aResponse().withStatus(200)
                .withBody(String.format("{\"_number\":%s}", versionCandidateId))))
    );

    final var request = CreateVersionRequest.builder()
        .name("request name")
        .description("request description")
        .build();

    mockMvc.perform(
        post("/versions/candidates")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(new ObjectMapper().writeValueAsString(request))
            .accept(MediaType.APPLICATION_JSON_VALUE)
    ).andExpectAll(
        status().isCreated(),
        content().contentType("application/json"),
        jsonPath("$.id", is(versionCandidateId)),
        jsonPath("$.name", is("request name")),
        jsonPath("$.description", is("request description")),
        jsonPath("$.hasConflicts", is(false)),
        jsonPath("$.creationDate", is("2022-10-28T15:34:00.000Z")),
        jsonPath("$.latestUpdate", is("2022-10-28T15:34:00.000Z")),
        jsonPath("$.author", is(context.getGerritProps().getUser())),
        jsonPath("$.validations", nullValue())
    );
    Thread.sleep(1000);
    Assertions.assertThat(context.getRepo(versionCandidateId)).exists();
  }
}
