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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.groups.model.GroupDetails;
import com.epam.digital.data.platform.management.groups.model.GroupListDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@DisplayName("Bp-Grouping in candidates version controller tests")
class CandidateVersionGroupsControllerIT extends BaseIT {

  @Test
  @DisplayName("GET /versions/candidates/business-process-groups should return 200 with groupsDetails")
  @SneakyThrows
  void getBusinessProcessGroups() {
    final var versionCandidateId = context.createVersionCandidate();
    mockMvc.perform(
        get("/versions/candidates/{versionCandidateId}/business-process-groups", versionCandidateId)
            .accept(MediaType.APPLICATION_JSON_VALUE)
    ).andExpectAll(
        status().isOk(),
        content().contentType(MediaType.APPLICATION_JSON),
        jsonPath("$.groups[0].name", is("111")),
        jsonPath("$.groups[0].processDefinitions", hasSize(0)),
        jsonPath("$.groups[1].name", is("222")),
        jsonPath("$.groups[1].processDefinitions", hasSize(0)),
        jsonPath("$.groups[2].name", is("333")),
        jsonPath("$.groups[2].processDefinitions", hasSize(0)),
        jsonPath("$.ungrouped", hasSize(0))
    );
  }

  @Test
  @DisplayName("should return 200 and save version candidate grouping details")
  @SneakyThrows
  void saveGroups() {
    // create version candidate
    final var versionCandidateId = context.createVersionCandidate();

    // perform request
    final var requestBody = GroupListDetails.builder()
        .groups(List.of(GroupDetails.builder().name("111")
                .processDefinitions(List.of("bp-1-process_definition_id", "bp-2-process_definition_id"))
                .build(),
            GroupDetails.builder().name("222")
                .processDefinitions(List.of("bp-3-process_definition_id")).build(),
            GroupDetails.builder().name("333").processDefinitions(new ArrayList<>()).build()))
        .ungrouped(List.of("bp-4-process_definition_id", "bp-5-process_definition_id")).build();


    mockMvc.perform(
        post("/versions/candidates/{versionCandidateId}/business-process-groups", versionCandidateId)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(requestBody))
    ).andExpectAll(
        status().isOk(),
        content().contentType(MediaType.APPLICATION_JSON),
        jsonPath("$.groups[0].name", is("111")),
        jsonPath("$.groups[0].processDefinitions", hasSize(0)),
        jsonPath("$.groups[1].name", is("222")),
        jsonPath("$.groups[1].processDefinitions", hasSize(0)),
        jsonPath("$.groups[2].name", is("333")),
        jsonPath("$.groups[2].processDefinitions", hasSize(0)),
        jsonPath("$.ungrouped", hasSize(0))
    );

    // define expected files contents
    final var expectedGroupingContent = context.getResourceContent(
        "/versions/candidates/{versionCandidateId}/bp-grouping/POST/bp-grouping.yml");

    final var actualGroupingContent = context.getFileFromRemoteVersionCandidateRepo(
        "bp-grouping/bp-grouping.yml");

    Assertions.assertThat(actualGroupingContent)
        .isEqualTo(expectedGroupingContent);
  }
}
