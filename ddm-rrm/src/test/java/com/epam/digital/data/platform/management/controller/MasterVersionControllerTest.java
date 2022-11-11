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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.versionmanagement.model.VersionInfoDto;
import com.epam.digital.data.platform.management.versionmanagement.service.VersionManagementService;
import java.time.LocalDateTime;
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

@ControllerTest(MasterVersionController.class)
@DisplayName("Master version controller tests")
class MasterVersionControllerTest {

  MockMvc mockMvc;
  @MockBean
  VersionManagementService versionManagementService;

  @BeforeEach
  void setUp(WebApplicationContext webApplicationContext,
      RestDocumentationContextProvider restDocumentation) {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
        .apply(documentationConfiguration(restDocumentation))
        .build();
  }

  @Test
  @DisplayName("GET /versions/master should return 200 with last merged change info")
  @SneakyThrows
  void getMaster() {
    var expectedChangeInfo = VersionInfoDto.builder()
        .number(1)
        .owner("JohnDoe@epam.com")
        .description("Version candidate to change form")
        .subject("JohnDoe's version candidate")
        .submitted(LocalDateTime.of(2022, 7, 29, 12, 31))
        .build();

    Mockito.doReturn(expectedChangeInfo)
        .when(versionManagementService).getMasterInfo();

    mockMvc.perform(
        get("/versions/master")
    ).andExpectAll(
        status().isOk(),
        content().contentType(MediaType.APPLICATION_JSON),
        jsonPath("$.id", is("1")),
        jsonPath("$.name", is("JohnDoe's version candidate")),
        jsonPath("$.description", is("Version candidate to change form")),
        jsonPath("$.author", is("JohnDoe@epam.com")),
        jsonPath("$.latestUpdate", is("2022-07-29T12:31:00.000Z")),
        jsonPath("$.published", nullValue()),
        jsonPath("$.inspector", nullValue()),
        jsonPath("$.validations", nullValue())
    ).andDo(document("versions/master/GET"));

    Mockito.verify(versionManagementService).getMasterInfo();
  }

  @Test
  @DisplayName("GET /versions/master should return 200 with empty object if there is no last merged change")
  @SneakyThrows
  void getMasterNoLastVersions() {
    Mockito.doReturn(null)
        .when(versionManagementService).getMasterInfo();

    mockMvc.perform(
        get("/versions/master")
    ).andExpectAll(
        status().isOk(),
        content().contentType(MediaType.APPLICATION_JSON),
        jsonPath("$.id", nullValue()),
        jsonPath("$.name", nullValue()),
        jsonPath("$.description", nullValue()),
        jsonPath("$.author", nullValue()),
        jsonPath("$.latestUpdate", nullValue()),
        jsonPath("$.published", nullValue()),
        jsonPath("$.inspector", nullValue()),
        jsonPath("$.validations", nullValue())
    ).andDo(document("versions/master/GET"));

    Mockito.verify(versionManagementService).getMasterInfo();
  }
}
