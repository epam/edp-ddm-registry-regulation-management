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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.settings.model.SettingsInfoDto;
import com.epam.digital.data.platform.management.settings.service.SettingServiceImpl;
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

@ControllerTest(MasterVersionSettingsController.class)
@DisplayName("Settings in version candidates controller tests")
class MasterVersionSettingsControllerTest {

  MockMvc mockMvc;
  @MockBean
  GerritPropertiesConfig gerritPropertiesConfig;
  @MockBean
  SettingServiceImpl settingService;

  @BeforeEach
  void setUp(WebApplicationContext webApplicationContext,
      RestDocumentationContextProvider restDocumentation) {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
        .apply(documentationConfiguration(restDocumentation))
        .build();
  }

  @Test
  @DisplayName("GET /versions/master/settings should return 200 with settings content")
  @SneakyThrows
  void getSettings() {
    final var headBranch = "head-branch";
    Mockito.doReturn(headBranch)
        .when(gerritPropertiesConfig).getHeadBranch();

    final var expectedSettingsInfo = SettingsInfoDto.builder()
        .themeFile("white-theme.js")
        .title("mdtuddm")
        .titleFull("<Назва реєстру>")
        .supportEmail("support@registry.gov.ua")
//        .blacklistedDomains(List.of("ya.ua", "ya.ru")) TODO uncomment after validator-cli update
        .build();
    Mockito.doReturn(expectedSettingsInfo)
        .when(settingService).getSettings(headBranch);

    mockMvc.perform(
        get("/versions/master/settings")
            .accept(MediaType.APPLICATION_JSON)
    ).andExpectAll(
        status().isOk(),
        content().contentType(MediaType.APPLICATION_JSON),
        jsonPath("$.titleFull", is("<Назва реєстру>")),
        jsonPath("$.title", is("mdtuddm")),
        jsonPath("$.themeFile", is("white-theme.js")),
        jsonPath("$.supportEmail", is("support@registry.gov.ua"))
//        jsonPath("$.blacklistedDomains", hasSize(2)) TODO uncomment after validator-cli update
    ).andDo(document("versions/master/settings/GET"));

    Mockito.verify(settingService).getSettings(headBranch);
  }
}