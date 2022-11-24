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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@DisplayName("Settings in master version controller tests")
class MasterVersionSettingsControllerIT extends BaseIT {

  @Test
  @DisplayName("GET /versions/master/settings should return 200 with all settings")
  @SneakyThrows
  void getSettings() {
    mockMvc.perform(
        get("/versions/master/settings")
            .accept(MediaType.APPLICATION_JSON_VALUE)
    ).andExpectAll(
        status().isOk(),
        content().contentType("application/json"),
        jsonPath("$.supportEmail", is("support@registry.gov.ua")),
        jsonPath("$.themeFile", is("white-theme.js")),
        jsonPath("$.titleFull", is("Registry full title")),
//      jsonPath("$.blacklistedDomains", hasSize(2)), TODO uncomment after validator-cli update
        jsonPath("title", is("Registry title"))
    );
  }
}
