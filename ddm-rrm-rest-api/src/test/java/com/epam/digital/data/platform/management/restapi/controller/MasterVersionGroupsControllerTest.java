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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.groups.model.BusinessProcessDefinition;
import com.epam.digital.data.platform.management.groups.model.BusinessProcessGroupsResponse;
import com.epam.digital.data.platform.management.groups.model.GroupDetailsResponse;
import com.epam.digital.data.platform.management.groups.service.GroupServiceImpl;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
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

@ControllerTest(MasterVersionGroupsController.class)
@DisplayName("Bp-Grouping in master version controller tests")
public class MasterVersionGroupsControllerTest {

  MockMvc mockMvc;
  @MockBean
  GerritPropertiesConfig gerritPropertiesConfig;
  @MockBean
  GroupServiceImpl groupService;

  @BeforeEach
  void setUp(WebApplicationContext webApplicationContext,
      RestDocumentationContextProvider restDocumentation) {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
        .apply(documentationConfiguration(restDocumentation))
        .build();
  }

  @Test
  @DisplayName("GET /versions/master/business-process-groups should return 200 with groupsDetails")
  @SneakyThrows
  void getBusinessProcessGroups() {
    final var headBranch = "head-branch";
    Mockito.doReturn(headBranch)
        .when(gerritPropertiesConfig).getHeadBranch();

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
            BusinessProcessDefinition.builder().id("bp-4-process_definition_id").name("John Doe added new component").build()))
        .build();

    Mockito.doReturn(expected)
        .when(groupService).getGroupsByVersion(headBranch);

    mockMvc.perform(
        get("/versions/master/business-process-groups")
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
    ).andDo(document("versions/master/business-process-groups/GET"));

    Mockito.verify(groupService).getGroupsByVersion(headBranch);
  }
}
