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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

@DisplayName("Master version controller tests")
class MasterVersionControllerIT extends BaseIT {

  @Test
  @DisplayName("GET /versions/master should return 200 with last merged change info")
  @SneakyThrows
  public void getMasterVersionInfo() {
    final var lastMergedChangeInfo = Map.of(
        "_number", 1,
        "owner", Map.of("username", context.getGerritProps().getUser()),
        "topic", "this is description for version candidate",
        "subject", "commit message",
        "submitted", "2022-08-02 16:15:12.786589626",
        "labels", Map.of(),
        "change_id", "change_id"
    );

    final var om = new ObjectMapper();
    context.getGerritMockServer().addStubMapping(stubFor(
        WireMock.get(urlEqualTo(String.format("/a/changes/?q=project:%s+status:merged+owner:%s&n=1",
                context.getGerritProps().getRepository(), context.getGerritProps().getUser())))
            .willReturn(aResponse().withStatus(200)
                .withBody(om.writeValueAsString(List.of(lastMergedChangeInfo))))
    ));
    context.getGerritMockServer().addStubMapping(stubFor(
        WireMock.get(urlPathEqualTo("/a/changes/change_id"))
            .willReturn(aResponse().withStatus(200)
                .withBody(om.writeValueAsString(lastMergedChangeInfo)))
    ));

    mockMvc.perform(
        get("/versions/master")
            .accept(MediaType.APPLICATION_JSON_VALUE)
    ).andExpectAll(
        status().isOk(),
        content().contentType("application/json"),
        jsonPath("$.id", is("1")),
        jsonPath("$.author", is(context.getGerritProps().getUser())),
        jsonPath("$.description", is("this is description for version candidate")),
        jsonPath("$.name", is("commit message")),
        jsonPath("$.latestUpdate", is("2022-08-02T16:15:12.000Z")),
        jsonPath("$.published", nullValue()),
        jsonPath("$.inspector", nullValue()),
        jsonPath("$.validations", nullValue())
    );
  }

  @Test
  @DisplayName("GET /versions/master should return 200 if there no merged changes")
  @SneakyThrows
  public void getMasterVersionInfo_noLastMergedMR() {
    context.getGerritMockServer().addStubMapping(stubFor(
        WireMock.get(urlEqualTo(String.format("/a/changes/?q=project:%s+status:merged+owner:%s&n=1",
                context.getGerritProps().getRepository(), context.getGerritProps().getUser())))
            .willReturn(aResponse().withStatus(200).withBody("[]"))
    ));

    mockMvc.perform(
        get("/versions/master")
            .accept(MediaType.APPLICATION_JSON_VALUE)
    ).andExpectAll(
        status().isOk(),
        content().contentType("application/json"),
        jsonPath("$.id", nullValue()),
        jsonPath("$.author", nullValue()),
        jsonPath("$.description", nullValue()),
        jsonPath("$.name", nullValue()),
        jsonPath("$.published", nullValue()),
        jsonPath("$.inspector", nullValue()),
        jsonPath("$.validations", nullValue())
    );
  }
}
