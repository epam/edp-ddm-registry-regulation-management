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

package com.epam.digital.data.platform.management.restapi.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.settings.model.SettingsInfoDto;
import com.epam.digital.data.platform.management.settings.service.SettingService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@ControllerTest(CandidateVersionSettingsController.class)
@DisplayName("Settings in version candidates controller tests")
class CandidateVersionSettingsControllerTest {

  @MockBean
  SettingService settingService;
  MockMvc mockMvc;

  @BeforeEach
  void setUp(WebApplicationContext webApplicationContext,
      RestDocumentationContextProvider restDocumentation) {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
        .apply(documentationConfiguration(restDocumentation))
        .build();
  }

  @Test
  @DisplayName("GET /versions/candidates/{versionCandidateId}/settings should return 200 with settings content")
  @SneakyThrows
  void getSettings() {
    final var expectedSettingsInfo = SettingsInfoDto.builder()
        .themeFile("white-theme.js")
        .title("mdtuddm")
        .titleFull("<Назва реєстру>")
        .supportEmail("support@registry.gov.ua")
//        .blacklistedDomains(List.of("ya.ua", "ya.ru")) TODO uncomment after validator-cli update
        .build();
    Mockito.doReturn(expectedSettingsInfo)
        .when(settingService).getSettings("1");

    mockMvc.perform(
        get("/versions/candidates/{versionCandidateId}/settings", "1")
            .accept(MediaType.APPLICATION_JSON)
    ).andExpectAll(
        status().isOk(),
        content().contentType("application/json"),
        jsonPath("$.titleFull", is("<Назва реєстру>")),
        jsonPath("$.title", is("mdtuddm")),
        jsonPath("$.themeFile", is("white-theme.js")),
        jsonPath("$.supportEmail", is("support@registry.gov.ua"))
//        jsonPath("$.blacklistedDomains", hasSize(2)) TODO uncomment after validator-cli update
    ).andDo(document("versions/candidates/{versionCandidateId}/settings/GET"));
    Mockito.verify(settingService).getSettings("1");
  }

  @Test
  @DisplayName("PUT /versions/candidates/{versionCandidateId}/settings should return 200 with settings content")
  @SneakyThrows
  void updateSettings() {
    final var expectedSettingsInfo = SettingsInfoDto.builder()
        .themeFile("white-theme.js")
        .title("mdtuddm")
        .titleFull("<Назва реєстру>")
        .supportEmail("support@registry.gov.ua")
//        .blacklistedDomains(List.of("ya.ua", "ya.ru")) TODO uncomment after validator-cli update
        .build();
    Mockito.doReturn(expectedSettingsInfo)
        .when(settingService).getSettings("1");

    mockMvc.perform(
        put("/versions/candidates/{versionCandidateId}/settings", "1")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(expectedSettingsInfo))
    ).andExpectAll(
        status().isOk(),
        content().contentType("application/json"),
        jsonPath("$.titleFull", is("<Назва реєстру>")),
        jsonPath("$.title", is("mdtuddm")),
        jsonPath("$.themeFile", is("white-theme.js")),
        jsonPath("$.supportEmail", is("support@registry.gov.ua"))
//        jsonPath("$.blacklistedDomains", hasSize(2)) TODO uncomment after validator-cli update
    ).andDo(document("versions/candidates/{versionCandidateId}/settings/PUT"));

    Mockito.verify(settingService).getSettings("1");
    Mockito.verify(settingService).updateSettings("1", expectedSettingsInfo);
  }
}