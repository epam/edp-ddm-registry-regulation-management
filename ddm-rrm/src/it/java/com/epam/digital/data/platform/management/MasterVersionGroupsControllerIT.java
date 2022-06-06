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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@DisplayName("Bp-Grouping in master version controller tests")
class MasterVersionGroupsControllerIT extends BaseIT {

  @Test
  @DisplayName("GET /versions/master/business-process-groups should return 200 with groupsDetails")
  @SneakyThrows
  void getBusinessProcessGroups() {
    mockMvc.perform(
        get("/versions/master/business-process-groups")
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
}
