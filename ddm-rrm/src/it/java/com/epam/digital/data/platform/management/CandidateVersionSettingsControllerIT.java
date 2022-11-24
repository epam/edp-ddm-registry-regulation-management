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

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@DisplayName("Settings in version candidates controller tests")
class CandidateVersionSettingsControllerIT extends BaseIT {

  @Nested
  @DisplayName("GET /versions/candidates/{versionCandidateId}/settings")
  class CandidateVersionSettingsGetSettingsControllerIT {

    @Test
    @DisplayName("should return 200 with version candidate settings")
    @SneakyThrows
    void getSettings() {
      // create version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // add files to version candidate remote
      final var globalVarsContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/settings/GET/camunda-global-system-vars.yml");
      context.addFileToVersionCandidateRemote(
          "/global-vars/camunda-global-system-vars.yml", globalVarsContent);
      final var settingsContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/settings/GET/settings.yml");
      context.addFileToVersionCandidateRemote("/settings/settings.yml",
          settingsContent);

      // perform request
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/settings", versionCandidateId)
              .accept(MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isOk(),
          content().contentType("application/json"),
          jsonPath("$.supportEmail", is("vc@registry.gov.ua")),
          jsonPath("$.themeFile", is("vc-white-theme.js")),
          jsonPath("$.titleFull", is("Version candidate Registry full title")),
//        jsonPath("$.blacklistedDomains", hasSize(2)), TODO uncomment after validator-cli update
          jsonPath("title", is("Version candidate Registry title"))
      );
    }

    @Test
    @DisplayName("should return 404 if version candidate doesn't exist")
    @SneakyThrows
    void getSettings_noVersionCandidate() {
      // create version candidate
      final var versionCandidateId = context.mockVersionCandidateDoesNotExist();

      // perform request
      mockMvc.perform(
          get("/versions/candidates/{versionCandidateId}/settings", versionCandidateId)
              .accept(MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isNotFound(),
          content().contentType("application/json"),
          jsonPath("$.code", is("CHANGE_NOT_FOUND")),
          jsonPath("$.details", is(String.format("Could not get change info for %s MR", versionCandidateId)))
      );
    }
  }

  @Nested
  @DisplayName("PUT /versions/candidates/{versionCandidateId}/settings")
  class CandidateVersionSettingsUpdateSettingsControllerIT {

    @Test
    @DisplayName("should return 200 and update version candidate settings")
    @SneakyThrows
    void getSettings() {
      // create version candidate
      final var versionCandidateId = context.createVersionCandidate();

      // perform request
      final var requestBody = Map.of(
          "supportEmail", "putvc@registry.gov.ua",
          "themeFile", "put-vc-white-theme.js",
          "titleFull", "PUT Version candidate Registry full title",
          "title", "PUT Version candidate Registry title"
      );
      mockMvc.perform(
          put("/versions/candidates/{versionCandidateId}/settings", versionCandidateId)
              .accept(MediaType.APPLICATION_JSON)
              .contentType(MediaType.APPLICATION_JSON)
              .content(new ObjectMapper().writeValueAsString(requestBody))
      ).andExpectAll(
          status().isOk(),
          content().contentType("application/json"),
          jsonPath("$.supportEmail", is("putvc@registry.gov.ua")),
          jsonPath("$.themeFile", is("put-vc-white-theme.js")),
          jsonPath("$.titleFull", is("PUT Version candidate Registry full title")),
//        jsonPath("$.blacklistedDomains", hasSize(2)), TODO uncomment after validator-cli update
          jsonPath("title", is("PUT Version candidate Registry title"))
      );

      // define expected files contents
      final var expectedGlobalVarsContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/settings/PUT/camunda-global-system-vars.yml");
      final var expectedSettingsContent = context.getResourceContent(
          "/versions/candidates/{versionCandidateId}/settings/PUT/settings.yml");
      // define actual file contents
      final var actualGlobalVarsContent = context.getFileFromRemoteVersionCandidateRepo(
          "global-vars/camunda-global-system-vars.yml");
      final var actualSettingsContent = context.getFileFromRemoteVersionCandidateRepo(
          "settings/settings.yml");

      Assertions.assertThat(actualGlobalVarsContent)
          .isEqualTo(expectedGlobalVarsContent);
      Assertions.assertThat(actualSettingsContent)
          .isEqualTo(expectedSettingsContent);
    }

    @Test
    @DisplayName("should return 404 if version candidate doesn't exist")
    @SneakyThrows
    void updateSettings_noVersionCandidate() {
      // create version candidate
      final var versionCandidateId = context.mockVersionCandidateDoesNotExist();

      // perform request
      final var requestBody = Map.of(
          "supportEmail", "putvc@registry.gov.ua",
          "themeFile", "put-vc-white-theme.js",
          "titleFull", "PUT Version candidate Registry full title",
          "title", "PUT Version candidate Registry title"
      );
      mockMvc.perform(
          put("/versions/candidates/{versionCandidateId}/settings", versionCandidateId)
              .accept(MediaType.APPLICATION_JSON)
              .contentType(MediaType.APPLICATION_JSON)
              .content(new ObjectMapper().writeValueAsString(requestBody))
      ).andExpectAll(
          status().isNotFound(),
          content().contentType("application/json"),
          jsonPath("$.code", is("CHANGE_NOT_FOUND")),
          jsonPath("$.details", is(String.format("Could not get change info for %s MR", versionCandidateId)))
      );
    }
  }
}
