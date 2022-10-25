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

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.model.dto.GlobalSettingsInfo;
import com.epam.digital.data.platform.management.service.VersionManagementService;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerTest(MasterVersionSettingsController.class)
class MasterVersionSettingsControllerTest {

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
  void getSettings() {
    GlobalSettingsInfo expected = GlobalSettingsInfo.builder()
        .themeFile("white-theme.js")
        .title("mdtuddm")
        .titleFull("<Назва реєстру>")
        .supportEmail("support@registry.gov.ua")
//        .blacklistedDomains(List.of("ya.ua", "ya.ru")) TODO uncomment after validator-cli update
        .build();
    Mockito.when(gerritPropertiesConfig.getHeadBranch()).thenReturn("head-branch");
    Mockito.when(globalSettingServiceImpl.getGlobalSettings("head-branch")).thenReturn(expected);

    mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/settings")
            .accept(MediaType.APPLICATION_JSON))
        .andExpectAll(
            status().isOk(),
            content().contentType("application/json"),
            jsonPath("$.titleFull", is("<Назва реєстру>")),
            jsonPath("$.title", is("mdtuddm")),
            jsonPath("$.themeFile", is("white-theme.js")),
            jsonPath("$.supportEmail", is("support@registry.gov.ua"))
//            jsonPath("$.blacklistedDomains", hasSize(2)) TODO uncomment after validator-cli update
        )
        .andDo(document("versions/master/settings/GET"));
    Mockito.verify(globalSettingServiceImpl).getGlobalSettings("head-branch");
  }
}