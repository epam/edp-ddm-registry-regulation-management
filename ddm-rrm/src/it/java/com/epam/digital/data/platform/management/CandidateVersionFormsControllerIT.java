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

package com.epam.digital.data.platform.management;

import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.core.config.JacksonConfig;
import com.google.gson.JsonParser;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("Forms in version candidates controller tests")
class CandidateVersionFormsControllerIT extends BaseIT {

  @Nested
  @DisplayName("GET /versions/candidates/{versionCandidateId}/forms/{formName}")
  class CandidateVersionFormsGetFormByNameControllerIT {

    @Test
    @DisplayName("should return 200 with form content")
    @SneakyThrows
    void getForm() {
      // add file to "remote" repo
      final var expectedFormContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/forms/{formName}/GET/john-does-form.json");
      context.addFileToRemoteHeadRepo("/forms/john-does-form.json", expectedFormContent);

      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // perform query
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/forms/{formName}",
              versionCandidateId, "john-does-form")
              .accept(MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON),
          content().json(expectedFormContent),
          header().string(HttpHeaders.ETAG, String.format("\"%s\"", expectedFormContent.hashCode()))
      );
    }

    @Test
    @DisplayName("should return 404 if version-candidate doesn't exist")
    @SneakyThrows
    void getForm_versionCandidateDoesNotExist() {
      // mock gerrit change info doesn't exist
      final var versionCandidateId = context.mockVersionCandidateDoesNotExist();

      // perform query
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/forms/{formName}",
              versionCandidateId, "john-does-form")
              .accept(MediaType.TEXT_XML, MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.code", is("CHANGE_NOT_FOUND")),
          jsonPath("$.details", is(String.format("Could not get change info for %s MR", versionCandidateId)))
      );
    }

    @Test
    @DisplayName("should return 404 if form doesn't exist")
    @SneakyThrows
    void getForm_formDoesNotExist() {
      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // perform query
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/forms/{formName}",
              versionCandidateId, "john-does-form")
              .accept(MediaType.TEXT_XML, MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.code", is("FORM_NOT_FOUND_EXCEPTION")),
          jsonPath("$.details", is("Form john-does-form not found"))
      );
    }
  }

  @Nested
  @DisplayName("GET /versions/candidates/{versionCandidateId}/forms")
  class CandidateVersionFormsGetFormListControllerIT {

    @Test
    @DisplayName("should return 200 with all found forms")
    @SneakyThrows
    void getFormsByVersionId() {
      // add files to "remote" repo
      final var johnDoesFormContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/forms/GET/john-does-form.json");
      context.addFileToRemoteHeadRepo("/forms/john-does-form.json", johnDoesFormContent);
      final var mrSmithsFormContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/forms/GET/mr-smiths-form.json");
      context.addFileToRemoteHeadRepo("/forms/mr-smiths-form.json", mrSmithsFormContent);

      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // define expected john-does-form dates
      final var expectedJohnDoesFormDates = context.getHeadRepoDatesByPath(
          "forms/john-does-form.json");

      // perform query
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/forms", versionCandidateId)
              .accept(MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$[0].name", is("john-does-form")),
          jsonPath("$[0].title", is("John Doe's form")),
          jsonPath("$[0].created", is(expectedJohnDoesFormDates.getCreated())),
          jsonPath("$[0].updated", is(expectedJohnDoesFormDates.getUpdated())),
          jsonPath("$[1].name", is("mr-smiths-form")),
          jsonPath("$[1].title", is("Mr Smith's form")),
          jsonPath("$[1].created", is("2022-10-28T20:21:48.845Z")),
          jsonPath("$[1].updated", is("2022-10-28T20:56:32.309Z"))
      );
    }

    @Test
    @DisplayName("should return 200 with empty array if there are no forms")
    @SneakyThrows
    void getFormsByVersionId_noForms() {
      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // perform query
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/forms", versionCandidateId)
              .accept(MediaType.APPLICATION_JSON_VALUE)
      ).andExpectAll(
          status().isOk(),
          content().contentType("application/json"),
          jsonPath("$", hasSize(0))
      );
    }

    @Test
    @DisplayName("should return 404 if version-candidate doesn't exist")
    @SneakyThrows
    void getFormsByVersionId_versionCandidateDoesNotExist() {
      // mock gerrit change info for version candidate
      final var versionCandidateId = context.mockVersionCandidateDoesNotExist();

      // perform query
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/forms", versionCandidateId)
              .accept(MediaType.APPLICATION_JSON_VALUE)
      ).andExpectAll(
          status().isNotFound(),
          content().contentType("application/json"),
          jsonPath("$.code", is("CHANGE_NOT_FOUND")),
          jsonPath("$.details", is(String.format("Could not get change info for %s MR", versionCandidateId)))
      );
    }
  }

  @Nested
  @DisplayName("POST /versions/candidates/{versionCandidateId}/forms/{formName}")
  class CandidateVersionFormsCreateFormByNameControllerIT {

    @Test
    @DisplayName("should return 200 and create form if there's no such form")
    @SneakyThrows
    void createForm() {
      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // define expected form content to create
      final var expectedFormContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/forms/{formName}/POST/valid-form.json");

      // perform query
      mockMvc.perform(
          post("/versions/candidates/{versionCandidateId}/forms/{formName}",
              versionCandidateId, "valid-form")
              .contentType(MediaType.APPLICATION_JSON)
              .content(expectedFormContent)
              .accept(MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isCreated(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.name", is("valid-form")),
          jsonPath("$.title", is("Valid form"))
      );

      // assert that actual content and expected have no differences except for created and updated dates
      final var actualFormContent = context.getFileFromRemoteVersionCandidateRepo(
          "/forms/valid-form.json");
      JSONAssert.assertEquals(expectedFormContent, actualFormContent,
          new CustomComparator(JSONCompareMode.LENIENT,
              new Customization("created", (o1, o2) -> true),
              new Customization("modified", (o1, o2) -> true)
          ));

      // assert that form dates are close to current date
      var form = JsonParser.parseString(actualFormContent).getAsJsonObject();
      final var created = LocalDateTime.parse(form.get("created").getAsString(),
          JacksonConfig.DATE_TIME_FORMATTER).format(JacksonConfig.DATE_TIME_FORMATTER);
      final var updated = LocalDateTime.parse(form.get("modified").getAsString(),
          JacksonConfig.DATE_TIME_FORMATTER).format(JacksonConfig.DATE_TIME_FORMATTER);
      Assertions.assertThat(LocalDateTime.parse(created, JacksonConfig.DATE_TIME_FORMATTER))
          .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
      Assertions.assertThat(LocalDateTime.parse(updated, JacksonConfig.DATE_TIME_FORMATTER))
          .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("should return 404 if version-candidate doesn't exist")
    @SneakyThrows
    void createForm_noVersionCandidate() {
      // mock gerrit change info for version candidate
      final var versionCandidateId = context.mockVersionCandidateDoesNotExist();

      // define expected form content to create
      final var expectedFormContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/forms/{formName}/POST/valid-form.json");

      // perform query
      mockMvc.perform(
          post("/versions/candidates/{versionCandidateId}/forms/{formName}",
              versionCandidateId, "valid-form")
              .contentType(MediaType.APPLICATION_JSON)
              .content(expectedFormContent)
              .accept(MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.code", is("CHANGE_NOT_FOUND")),
          jsonPath("$.details", is(String.format("Could not get change info for %s MR", versionCandidateId)))
      );
    }

    @Test
    @DisplayName("should return 409 if there's already exists such form")
    @SneakyThrows
    void createForm_formAlreadyExists() {
      // add file to "remote" repo
      final var expectedFormContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/forms/{formName}/POST/valid-form.json");
      context.addFileToRemoteHeadRepo("/forms/valid-form.json", expectedFormContent);

      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // perform query
      mockMvc.perform(
          post("/versions/candidates/{versionCandidateId}/forms/{formName}",
              versionCandidateId, "valid-form")
              .contentType(MediaType.APPLICATION_JSON)
              .content(expectedFormContent)
              .accept(MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isConflict(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.code", is("FORM_ALREADY_EXISTS_EXCEPTION")),
          jsonPath("$.details", is("Form with path 'forms/valid-form.json' already exists"))
      );
    }

    @Test
    @DisplayName("should get form added to remote after creating form")
    @SneakyThrows
    void shouldUpdateLocalRepoWhenCreateForm() {
      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // define expected form content to create
      final var expectedFormContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/forms/{formName}/POST/valid-form.json");
      final var formToCreate = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/forms/{formName}/POST/valid-form-2.json");
      context.addFileToVersionCandidateRemote("/forms/valid-form.json", expectedFormContent);

      // perform query
      mockMvc.perform(
          post("/versions/candidates/{versionCandidateId}/forms/{formName}",
              versionCandidateId, "valid-form-2")
              .contentType(MediaType.APPLICATION_JSON)
              .content(formToCreate)
              .accept(MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isCreated(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.name", is("valid-form-2")),
          jsonPath("$.title", is("Valid form"))
      );

      // assert that actual content and expected have no differences except for created and updated dates
      final var actualFormContent = context.getFileFromRemoteVersionCandidateRepo(
          "/forms/valid-form.json");
      JSONAssert.assertEquals(expectedFormContent, actualFormContent,
          new CustomComparator(JSONCompareMode.LENIENT,
              new Customization("created", (o1, o2) -> true),
              new Customization("modified", (o1, o2) -> true)
          ));
    }
  }

  @Nested
  @DisplayName("PUT /versions/candidates/{versionCandidateId}/forms/{formName}")
  class CandidateVersionFormsUpdateFormByNameControllerIT {

    @Test
    @DisplayName("should return 200 and update form if there's already exists such form")
    @SneakyThrows
    void updateForm_noETag() {
      // add file to "remote" repo
      final var headFormContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/forms/{formName}/PUT/valid-form-head.json");
      context.addFileToRemoteHeadRepo("/forms/valid-form.json", headFormContent);

      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // define expected form content to update
      final var expectedFormContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/forms/{formName}/PUT/valid-form-version-candidate.json");

      // perform query
      mockMvc.perform(
          put("/versions/candidates/{versionCandidateId}/forms/{formName}",
              versionCandidateId, "valid-form")
              .contentType(MediaType.APPLICATION_JSON)
              .content(expectedFormContent)
              .accept(MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.name", is("valid-form")),
          jsonPath("$.title", is("Valid form Version Candidate"))
      );

      // define expected created date for form
      final var expectedCreated = context.getHeadRepoDatesByPath(
          "forms/valid-form.json").getCreated();

      // assert that actual content and expected have no differences except for created and updated dates
      final var actualFormContent = context.getFileFromRemoteVersionCandidateRepo(
          "/forms/valid-form.json");

      JSONAssert.assertEquals(expectedFormContent, actualFormContent,
          new CustomComparator(JSONCompareMode.LENIENT,
              new Customization("created", (o1, o2) -> true),
              new Customization("modified", (o1, o2) -> true)
          ));

      // assert that form dates are close to current date
      var form = JsonParser.parseString(actualFormContent).getAsJsonObject();
      final var created = LocalDateTime.parse(form.get("created").getAsString(),
          JacksonConfig.DATE_TIME_FORMATTER).format(JacksonConfig.DATE_TIME_FORMATTER);
      final var updated = LocalDateTime.parse(form.get("modified").getAsString(),
          JacksonConfig.DATE_TIME_FORMATTER).format(JacksonConfig.DATE_TIME_FORMATTER);
      Assertions.assertThat(created)
          .isEqualTo(expectedCreated);
      Assertions.assertThat(LocalDateTime.parse(updated, JacksonConfig.DATE_TIME_FORMATTER))
          .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
    }


    @Test
    @DisplayName("should return 200 and update form if there's already exists such form")
    @SneakyThrows
    void updateForm_validETag() {
      // add file to "remote" repo
      final var headFormContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/forms/{formName}/PUT/valid-form-head.json");
      context.addFileToRemoteHeadRepo("/forms/valid-form.json", headFormContent);

      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // define expected form content to update
      final var expectedFormContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/forms/{formName}/PUT/valid-form-version-candidate.json");

      //perform get
      MockHttpServletResponse response = mockMvc.perform(get("/versions/candidates/{versionCandidateId}/forms/{formName}",
          versionCandidateId, "valid-form")).andReturn().getResponse();

      //get eTag value from response
      String eTag = response.getHeader("ETag");

      // perform query
      mockMvc.perform(
          put("/versions/candidates/{versionCandidateId}/forms/{formName}",
              versionCandidateId, "valid-form")
              .contentType(MediaType.APPLICATION_JSON)
              .content(expectedFormContent)
              .header("If-Match", eTag)
              .accept(MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.name", is("valid-form")),
          jsonPath("$.title", is("Valid form Version Candidate"))
      );

      // define expected created date for form
      final var expectedCreated = context.getHeadRepoDatesByPath(
          "forms/valid-form.json").getCreated();

      // assert that actual content and expected have no differences except for created and updated dates
      final var actualFormContent = context.getFileFromRemoteVersionCandidateRepo(
          "/forms/valid-form.json");

      JSONAssert.assertEquals(expectedFormContent, actualFormContent,
          new CustomComparator(JSONCompareMode.LENIENT,
              new Customization("created", (o1, o2) -> true),
              new Customization("modified", (o1, o2) -> true)
          ));

      // assert that form dates are close to current date
      var form = JsonParser.parseString(actualFormContent).getAsJsonObject();
      final var created = LocalDateTime.parse(form.get("created").getAsString(),
          JacksonConfig.DATE_TIME_FORMATTER).format(JacksonConfig.DATE_TIME_FORMATTER);
      final var updated = LocalDateTime.parse(form.get("modified").getAsString(),
          JacksonConfig.DATE_TIME_FORMATTER).format(JacksonConfig.DATE_TIME_FORMATTER);
      Assertions.assertThat(created)
          .isEqualTo(expectedCreated);
      Assertions.assertThat(LocalDateTime.parse(updated, JacksonConfig.DATE_TIME_FORMATTER))
          .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
    }


    @Test
    @DisplayName("should return 200 and update form with asterisk ETag")
    @SneakyThrows
    void updateForm_asteriskETag() {
      // add file to "remote" repo
      final var headFormContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/forms/{formName}/PUT/valid-form-head.json");
      context.addFileToRemoteHeadRepo("/forms/valid-form.json", headFormContent);

      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // define expected form content to update
      final var expectedFormContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/forms/{formName}/PUT/valid-form-version-candidate.json");

      // perform query
      mockMvc.perform(
          put("/versions/candidates/{versionCandidateId}/forms/{formName}",
              versionCandidateId, "valid-form")
              .contentType(MediaType.APPLICATION_JSON)
              .content(expectedFormContent)
              .header("If-Match", "*")
              .accept(MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.name", is("valid-form")),
          jsonPath("$.title", is("Valid form Version Candidate"))
      );

      // define expected created date for form
      final var expectedCreated = context.getHeadRepoDatesByPath(
          "forms/valid-form.json").getCreated();

      // assert that actual content and expected have no differences except for created and updated dates
      final var actualFormContent = context.getFileFromRemoteVersionCandidateRepo(
          "/forms/valid-form.json");

      JSONAssert.assertEquals(expectedFormContent, actualFormContent,
          new CustomComparator(JSONCompareMode.LENIENT,
              new Customization("created", (o1, o2) -> true),
              new Customization("modified", (o1, o2) -> true)
          ));

      // assert that form dates are close to current date
      var form = JsonParser.parseString(actualFormContent).getAsJsonObject();
      final var created = LocalDateTime.parse(form.get("created").getAsString(),
          JacksonConfig.DATE_TIME_FORMATTER).format(JacksonConfig.DATE_TIME_FORMATTER);
      final var updated = LocalDateTime.parse(form.get("modified").getAsString(),
          JacksonConfig.DATE_TIME_FORMATTER).format(JacksonConfig.DATE_TIME_FORMATTER);
      Assertions.assertThat(created)
          .isEqualTo(expectedCreated);
      Assertions.assertThat(LocalDateTime.parse(updated, JacksonConfig.DATE_TIME_FORMATTER))
          .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("should return 200 and create form if there's no such form")
    @SneakyThrows
    void updateForm_noFormsToUpdate() {
      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // define expected form content to create
      final var expectedFormContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/forms/{formName}/PUT/valid-form-version-candidate.json");

      // perform query
      mockMvc.perform(
          put("/versions/candidates/{versionCandidateId}/forms/{formName}",
              versionCandidateId, "valid-form")
              .contentType(MediaType.APPLICATION_JSON)
              .content(expectedFormContent)
              .accept(MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.name", is("valid-form")),
          jsonPath("$.title", is("Valid form Version Candidate"))
      );

      // assert that actual content and expected have no differences except for created and updated dates
      final var actualFormContent = context.getFileFromRemoteVersionCandidateRepo(
          "/forms/valid-form.json");
      JSONAssert.assertEquals(expectedFormContent, actualFormContent,
          new CustomComparator(JSONCompareMode.LENIENT,
              new Customization("created", (o1, o2) -> true),
              new Customization("modified", (o1, o2) -> true)
          ));

      // assert that form dates are close to current date
      var form = JsonParser.parseString(actualFormContent).getAsJsonObject();
      final var created = LocalDateTime.parse(form.get("created").getAsString(),
          JacksonConfig.DATE_TIME_FORMATTER).format(JacksonConfig.DATE_TIME_FORMATTER);
      final var updated = LocalDateTime.parse(form.get("modified").getAsString(),
          JacksonConfig.DATE_TIME_FORMATTER).format(JacksonConfig.DATE_TIME_FORMATTER);
      Assertions.assertThat(LocalDateTime.parse(created, JacksonConfig.DATE_TIME_FORMATTER))
          .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
      Assertions.assertThat(LocalDateTime.parse(updated, JacksonConfig.DATE_TIME_FORMATTER))
          .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("should return 404 if version-candidate doesn't exist")
    @SneakyThrows
    void updateForm_noVersionCandidate() {
      // mock gerrit change info for version candidate
      final var versionCandidateId = context.mockVersionCandidateDoesNotExist();

      // define expected form content to update
      final var expectedFormContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/forms/{formName}/PUT/valid-form-version-candidate.json");

      // perform query
      mockMvc.perform(
          put("/versions/candidates/{versionCandidateId}/forms/{formName}",
              versionCandidateId, "valid-form")
              .contentType(MediaType.APPLICATION_JSON)
              .content(expectedFormContent)
              .accept(MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.code", is("CHANGE_NOT_FOUND")),
          jsonPath("$.details", is(String.format("Could not get change info for %s MR", versionCandidateId)))
      );
    }

    @Test
    @DisplayName("should return 412 if wrong ETag")
    @SneakyThrows
    void updateForm_invalidETag() {
      // add file to "remote" repo
      final var headFormContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/forms/{formName}/PUT/valid-form-head.json");
      context.addFileToRemoteHeadRepo("/forms/valid-form.json", headFormContent);

      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // define expected form content to update
      final var expectedFormContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/forms/{formName}/PUT/valid-form-version-candidate.json");

      // perform query
      mockMvc.perform(
          put("/versions/candidates/{versionCandidateId}/forms/{formName}",
              versionCandidateId, "valid-form")
              .contentType(MediaType.APPLICATION_JSON)
              .content(expectedFormContent)
              .header("If-Match", RandomString.make())
              .accept(MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isConflict()
    );

      // assert that actual content was not updated
      final var actualFormContent = context.getFileFromRemoteVersionCandidateRepo(
          "/forms/valid-form.json");

      JSONAssert.assertNotEquals(expectedFormContent, actualFormContent,
          new CustomComparator(JSONCompareMode.LENIENT,
              new Customization("created", (o1, o2) -> true),
              new Customization("modified", (o1, o2) -> true)
          ));
    }

    @Test
    @DisplayName("should return 412 if modified concurrently")
    @SneakyThrows
    void updateForm_modifiedConcurrently() {
      // add file to "remote" repo
      final var headFormContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/forms/{formName}/PUT/valid-form-head.json");
      context.addFileToRemoteHeadRepo("/forms/valid-form.json", headFormContent);

      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // define expected form content to update
      final var expectedFormContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/forms/{formName}/PUT/valid-form-version-candidate.json");

      // define modified form content to update
      final var modifiedFormContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/forms/{formName}/PUT/valid-form-version-candidate-modified.json");

      //perform get
      MockHttpServletResponse response = mockMvc.perform(get("/versions/candidates/{versionCandidateId}/forms/{formName}",
          versionCandidateId, "valid-form")).andReturn().getResponse();

      //get eTag value from response
      String eTag = response.getHeader("ETag");

      //perform update with missing eTag
      mockMvc.perform(
          put("/versions/candidates/{versionCandidateId}/forms/{formName}",
              versionCandidateId, "valid-form")
              .contentType(MediaType.APPLICATION_JSON)
              .content(modifiedFormContent)
              .accept(MediaType.APPLICATION_JSON));

      // perform query with outdated ETag
      mockMvc.perform(
          put("/versions/candidates/{versionCandidateId}/forms/{formName}",
              versionCandidateId, "valid-form")
              .contentType(MediaType.APPLICATION_JSON)
              .content(expectedFormContent)
              .header("If-Match", eTag)
              .accept(MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isConflict()
      );

      // assert that actual content was not updated after second request
      final var actualFormContent = context.getFileFromRemoteVersionCandidateRepo(
          "/forms/valid-form.json");
      JSONAssert.assertEquals(modifiedFormContent, actualFormContent,
          new CustomComparator(JSONCompareMode.LENIENT,
              new Customization("created", (o1, o2) -> true),
              new Customization("modified", (o1, o2) -> true)
          ));
    }
  }

  @Nested
  @DisplayName("DELETE /versions/candidates/{versionCandidateId}/forms/{formName}")
  class CandidateVersionFormsDeleteFormByNameControllerIT {

    @Test
    @DisplayName("should return 204 and delete form if there's already exists such form")
    @SneakyThrows
    void deleteForm_noETag() {
      // add file to "remote" repo
      final var headFormContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/forms/{formName}/DELETE/john-does-form.json");
      context.addFileToRemoteHeadRepo("/forms/john-does-form.json", headFormContent);

      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // perform query
      mockMvc.perform(delete(
          "/versions/candidates/{versionCandidateId}/forms/{formName}",
          versionCandidateId, "john-does-form")
      ).andExpect(
          status().isNoContent()
      );

      // assert that file is deleted
      final var isFileExists = context.isFileExistsInRemoteVersionCandidateRepo(
          "/forms/john-does-form.json");
      Assertions.assertThat(isFileExists).isFalse();
    }

    @Test
    @DisplayName("should return 204 and delete form if there's already exists such form")
    @SneakyThrows
    void deleteForm_validETag() {
      // add file to "remote" repo
      final var headFormContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/forms/{formName}/DELETE/john-does-form.json");
      context.addFileToRemoteHeadRepo("/forms/john-does-form.json", headFormContent);

      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      //perform get
      MockHttpServletResponse response = mockMvc.perform(get("/versions/candidates/{versionCandidateId}/forms/{formName}",
          versionCandidateId, "john-does-form")).andReturn().getResponse();

      //get eTag value from response
      String eTag = response.getHeader("ETag");

      // perform query
      mockMvc.perform(delete(
          "/versions/candidates/{versionCandidateId}/forms/{formName}",
          versionCandidateId, "john-does-form")
          .header("If-Match", eTag)
      ).andExpect(
          status().isNoContent()
      );

      // assert that file is deleted
      final var isFileExists = context.isFileExistsInRemoteVersionCandidateRepo(
          "/forms/john-does-form.json");
      Assertions.assertThat(isFileExists).isFalse();
    }

    @Test
    @DisplayName("should return 412 with invalid ETag")
    @SneakyThrows
    void deleteForm_invalidETag() {
      // add file to "remote" repo
      final var headFormContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/forms/{formName}/DELETE/john-does-form.json");
      context.addFileToRemoteHeadRepo("/forms/john-does-form.json", headFormContent);

      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // perform query
      mockMvc.perform(delete(
          "/versions/candidates/{versionCandidateId}/forms/{formName}",
          versionCandidateId, "john-does-form")
          .header("If-Match", RandomString.make())
      ).andExpect(
          status().isConflict()
      );

      // assert that file was not deleted
      final var isFileExists = context.isFileExistsInRemoteVersionCandidateRepo(
          "/forms/john-does-form.json");
      Assertions.assertThat(isFileExists).isTrue();
    }


    @Test
    @DisplayName("should return 204 and delete form with asterisk ETag")
    @SneakyThrows
    void deleteForm_asteriskETag() {
      // add file to "remote" repo
      final var headFormContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/forms/{formName}/DELETE/john-does-form.json");
      context.addFileToRemoteHeadRepo("/forms/john-does-form.json", headFormContent);

      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // perform query
      mockMvc.perform(delete(
          "/versions/candidates/{versionCandidateId}/forms/{formName}",
          versionCandidateId, "john-does-form")
          .header("If-Match", "*")
      ).andExpect(
          status().isNoContent()
      );

      // assert that file is deleted
      final var isFileExists = context.isFileExistsInRemoteVersionCandidateRepo(
          "/forms/john-does-form.json");
      Assertions.assertThat(isFileExists).isFalse();
    }

    @Test
    @DisplayName("should return 204 if there's no such form")
    @SneakyThrows
    void deleteForm_noFormToDelete() {
      // mock gerrit change info for version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // perform query
      mockMvc.perform(
          delete(
              "/versions/candidates/{versionCandidateId}/forms/{formName}",
              versionCandidateId, "john-does-form")
      ).andExpect(
          status().isNoContent()
      );
    }

    @Test
    @DisplayName("should return 404 if version-candidate doesn't exist")
    @SneakyThrows
    void deleteForm_noVersionCandidate() {
      // mock gerrit change info for version candidate
      final var versionCandidateId = context.mockVersionCandidateDoesNotExist();

      // perform query
      mockMvc.perform(
          delete(
              "/versions/candidates/{versionCandidateId}/forms/{formName}",
              versionCandidateId, "valid-form")
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.code", is("CHANGE_NOT_FOUND")),
          jsonPath("$.details", is(String.format("Could not get change info for %s MR", versionCandidateId)))
      );
    }
  }
}
