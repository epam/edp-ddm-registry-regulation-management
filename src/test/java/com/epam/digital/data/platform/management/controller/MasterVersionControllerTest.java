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

package com.epam.digital.data.platform.management.controller;

import static com.epam.digital.data.platform.management.controller.CandidateVersionControllerTest.VERSION_CANDIDATE_AUTHOR;
import static com.epam.digital.data.platform.management.controller.CandidateVersionControllerTest.VERSION_CANDIDATE_DESCRIPTION;
import static com.epam.digital.data.platform.management.controller.CandidateVersionControllerTest.VERSION_CANDIDATE_NAME;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.model.dto.ChangeInfoDetailedDto;
import com.epam.digital.data.platform.management.service.VersionManagementService;
import java.time.LocalDateTime;

import com.epam.digital.data.platform.management.service.impl.GlobalSettingServiceImpl;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ControllerTest(MasterVersionController.class)
class MasterVersionControllerTest {

  static final String BASE_URL = "/versions/master";

  MockMvc mockMvc;
  @MockBean
  private GerritPropertiesConfig gerritPropertiesConfig;
  @MockBean
  private VersionManagementService versionManagementService;
  @MockBean
  private GlobalSettingServiceImpl globalSettingServiceImpl;
  @RegisterExtension
  final RestDocumentationExtension restDocumentation = new RestDocumentationExtension();

  @BeforeEach
  public void setUp(WebApplicationContext webApplicationContext,
      RestDocumentationContextProvider restDocumentation) {

    this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
        .apply(documentationConfiguration(restDocumentation)).build();
  }

  @Test
  @SneakyThrows
  void getMaster() {
    var expectedChangeInfo = ChangeInfoDetailedDto.builder()
        .number(1)
        .owner(VERSION_CANDIDATE_AUTHOR)
        .description(VERSION_CANDIDATE_DESCRIPTION)
        .subject(VERSION_CANDIDATE_NAME)
        .submitted(LocalDateTime.of(2022, 7, 29, 12, 31))
        .build();
    Mockito.when(versionManagementService.getMasterInfo()).thenReturn(expectedChangeInfo);

    mockMvc.perform(get(BASE_URL))
        .andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.id", is("1")),
            jsonPath("$.name", is(VERSION_CANDIDATE_NAME)),
            jsonPath("$.description", is(VERSION_CANDIDATE_DESCRIPTION)),
            jsonPath("$.author", is(VERSION_CANDIDATE_AUTHOR)),
            jsonPath("$.latestUpdate", is("2022-07-29T12:31:00.000Z")),
            jsonPath("$.published", nullValue()),
            jsonPath("$.inspector", nullValue()),
            jsonPath("$.validations", nullValue()))
        .andDo(document("versions/master/GET"));
  }

  @Test
  @SneakyThrows
  void getMasterNoLastVersions() {
    Mockito.when(versionManagementService.getMasterInfo()).thenReturn(null);

    mockMvc.perform(get(BASE_URL))
        .andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.id", nullValue()),
            jsonPath("$.name", nullValue()),
            jsonPath("$.description", nullValue()),
            jsonPath("$.author", nullValue()),
            jsonPath("$.latestUpdate", nullValue()),
            jsonPath("$.published", nullValue()),
            jsonPath("$.inspector", nullValue()),
            jsonPath("$.validations", nullValue()))
        .andDo(document("versions/master/GET"));
  }
}
