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

package com.epam.digital.data.platform.management.restapi.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.groups.model.BusinessProcessDefinition;
import com.epam.digital.data.platform.management.groups.model.BusinessProcessGroupsResponse;
import com.epam.digital.data.platform.management.groups.model.GroupDetails;
import com.epam.digital.data.platform.management.groups.model.GroupDetailsResponse;
import com.epam.digital.data.platform.management.groups.model.GroupListDetails;
import com.epam.digital.data.platform.management.groups.service.GroupServiceImpl;
import com.epam.digital.data.platform.management.groups.validation.BpGroupingValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ControllerTest(CandidateVersionGroupsController.class)
@DisplayName("Bp-Grouping in version candidates controller tests")
class CandidateVersionGroupsControllerTest {

  @MockBean
  BpGroupingValidator validator;
  @MockBean
  GroupServiceImpl groupService;
  MockMvc mockMvc;

  @BeforeEach
  void setUp(WebApplicationContext webApplicationContext,
      RestDocumentationContextProvider restDocumentation) {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
        .apply(documentationConfiguration(restDocumentation))
        .build();
  }

  @Test
  @DisplayName("GET /versions/candidates/{versionCandidateId}/business-process-groups should return 200 with groupsDetails")
  @SneakyThrows
  void getGrouping() {
    var versionId = RandomString.make();
    final BusinessProcessGroupsResponse expected = BusinessProcessGroupsResponse.builder()
        .groups(List.of(GroupDetailsResponse.builder()
                .name("Перша група")
                .processDefinitions(new ArrayList<>())
                .build(),
            GroupDetailsResponse.builder()
                .name("Друга група")
                .processDefinitions(new ArrayList<>())
                .build(),
            GroupDetailsResponse.builder().name("Третя група").processDefinitions(new ArrayList<>())
                .build()))
        .ungrouped(List.of(
            BusinessProcessDefinition.builder().id("bp-4-process_definition_id")
                .name("John Doe added new component").build()))
        .build();

    Mockito.doReturn(expected)
        .when(groupService).getGroupsByVersion(versionId);

    mockMvc.perform(
        get("/versions/candidates/{versionCandidateId}/business-process-groups", versionId)
            .accept(MediaType.APPLICATION_JSON)
    ).andExpectAll(
        status().isOk(),
        content().contentType(MediaType.APPLICATION_JSON),
        jsonPath("$.groups[0].name", is("Перша група")),
        jsonPath("$.groups[0].processDefinitions", hasSize(0)),
        jsonPath("$.groups[1].name", is("Друга група")),
        jsonPath("$.groups[1].processDefinitions", hasSize(0)),
        jsonPath("$.groups[2].name", is("Третя група")),
        jsonPath("$.groups[2].processDefinitions", hasSize(0)),
        jsonPath("$.ungrouped[0].id", is("bp-4-process_definition_id")),
        jsonPath("$.ungrouped[0].name", is("John Doe added new component"))
    ).andDo(document("versions/candidates/{versionCandidateId}/business-process-groups/GET"));
    Mockito.verify(groupService).getGroupsByVersion(versionId);
  }

  @Test
  @DisplayName("GET /versions/candidates/{versionCandidateId}/business-process-groups should return 200 with groupsDetails")
  @SneakyThrows
  void getGroupingWithProcessDefinitions() {
    var versionId = RandomString.make();
    final var expected = BusinessProcessGroupsResponse.builder()
        .groups(List.of(GroupDetailsResponse.builder()
                .name("Перша група")
                .processDefinitions(List.of(BusinessProcessDefinition.builder().name("Process_1")
                    .id("bp-1-process_definition_id").build()))
                .build(),
            GroupDetailsResponse.builder()
                .name("Друга група")
                .processDefinitions(List.of(
                    BusinessProcessDefinition.builder().name("Process_2")
                        .id("bp-2-process_definition_id").build(),
                    BusinessProcessDefinition.builder().name("Process_3")
                        .id("bp-3-process_definition_id").build()))
                .build(),
            GroupDetailsResponse.builder().name("Третя група").processDefinitions(new ArrayList<>())
                .build()))
        .ungrouped(List.of(
            BusinessProcessDefinition.builder().id("bp-4-process_definition_id")
                .name("John Doe added new component").build()))
        .build();

    Mockito.doReturn(expected)
        .when(groupService).getGroupsByVersion(versionId);

    mockMvc.perform(
        get("/versions/candidates/{versionCandidateId}/business-process-groups", versionId)
            .accept(MediaType.APPLICATION_JSON)
    ).andExpectAll(
        status().isOk(),
        content().contentType(MediaType.APPLICATION_JSON),
        jsonPath("$.groups[0].name", is("Перша група")),
        jsonPath("$.groups[0].processDefinitions[0].id", is("bp-1-process_definition_id")),
        jsonPath("$.groups[0].processDefinitions[0].name", is("Process_1")),
        jsonPath("$.groups[1].name", is("Друга група")),
        jsonPath("$.groups[1].processDefinitions[0].id", is("bp-2-process_definition_id")),
        jsonPath("$.groups[1].processDefinitions[0].name", is("Process_2")),
        jsonPath("$.groups[1].processDefinitions[1].id", is("bp-3-process_definition_id")),
        jsonPath("$.groups[1].processDefinitions[1].name", is("Process_3")),
        jsonPath("$.groups[2].name", is("Третя група")),
        jsonPath("$.groups[2].processDefinitions", hasSize(0)),
        jsonPath("$.ungrouped[0].id", is("bp-4-process_definition_id")),
        jsonPath("$.ungrouped[0].name", is("John Doe added new component"))
    ).andDo(document("versions/candidates/{versionCandidateId}/business-process-groups/GET"));
    Mockito.verify(groupService).getGroupsByVersion(versionId);
  }

  @Test
  @DisplayName("POST /versions/candidates/{versionCandidateId}/settings should return 200 with groupsDetails")
  @SneakyThrows
  void saveGrouping() {
    var versionId = RandomString.make();
    final var expected = GroupListDetails.builder()
        .groups(List.of(GroupDetails.builder().name("Перша група")
                .processDefinitions(List.of("bp-1-process_definition_id", "bp-2-process_definition_id"))
                .build(),
            GroupDetails.builder().name("Друга група")
                .processDefinitions(List.of("bp-3-process_definition_id")).build(),
            GroupDetails.builder().name("Третя група").build()))
        .ungrouped(List.of("bp-4-process_definition_id", "bp-5-process_definition_id")).build();

    final BusinessProcessGroupsResponse expectedGet = BusinessProcessGroupsResponse.builder()
        .groups(List.of(GroupDetailsResponse.builder()
                .name("Перша група")
                .processDefinitions(new ArrayList<>())
                .build(),
            GroupDetailsResponse.builder()
                .name("Друга група")
                .processDefinitions(new ArrayList<>())
                .build(),
            GroupDetailsResponse.builder().name("Третя група").processDefinitions(new ArrayList<>())
                .build()))
        .ungrouped(List.of(
            BusinessProcessDefinition.builder().id("bp-4-process_definition_id")
                .name("John Doe added new component").build()))
        .build();
    Mockito.doReturn(expectedGet)
        .when(groupService).getGroupsByVersion(versionId);

    mockMvc.perform(
        post("/versions/candidates/{versionCandidateId}/business-process-groups", versionId)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(expected))
    ).andExpectAll(
        status().isOk(),
        content().contentType(MediaType.APPLICATION_JSON),
        jsonPath("$.groups[0].name", is("Перша група")),
        jsonPath("$.groups[0].processDefinitions", hasSize(0)),
        jsonPath("$.groups[1].name", is("Друга група")),
        jsonPath("$.groups[1].processDefinitions", hasSize(0)),
        jsonPath("$.groups[2].name", is("Третя група")),
        jsonPath("$.groups[2].processDefinitions", hasSize(0)),
        jsonPath("$.ungrouped[0].id", is("bp-4-process_definition_id")),
        jsonPath("$.ungrouped[0].name", is("John Doe added new component"))
    ).andDo(document("versions/candidates/{versionCandidateId}/settings/POST"));

    Mockito.verify(groupService).save(versionId, expected);
    Mockito.verify(groupService).getGroupsByVersion(versionId);
  }

  @Test
  @DisplayName("POST /versions/candidates/{versionCandidateId}/business-process-groups/rollback should return 200")
  @SneakyThrows
  void rollbackBusinessProcessGroupsTest() {
    var versionId = RandomString.make();
    Mockito.doNothing().when(groupService).rollbackBusinessProcessGroups(versionId);

    mockMvc.perform(
        post("/versions/candidates/{versionCandidateId}/business-process-groups/rollback",
            versionId)
    ).andExpect(
        status().isOk()
    ).andDo(
        document("versions/candidates/{versionCandidateId}/business-process-groups/rollback/POST"));

    Mockito.verify(groupService).rollbackBusinessProcessGroups(versionId);
  }
}