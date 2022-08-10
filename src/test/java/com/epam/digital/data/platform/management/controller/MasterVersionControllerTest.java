package com.epam.digital.data.platform.management.controller;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.model.dto.ChangeInfo;
import com.epam.digital.data.platform.management.service.VersionManagementService;
import java.time.LocalDateTime;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ControllerTest(MasterVersionController.class)
class MasterVersionControllerTest {

  static final String BASE_URL = "/versions/master";

  @Autowired
  MockMvc mockMvc;

  @MockBean
  VersionManagementService versionManagementService;

  @Test
  @SneakyThrows
  void getMaster() {
    var expectedChangeInfo = ChangeInfo.builder()
        .number(1)
        .owner("owner@epam.com")
        .description("description")
        .subject("name")
        .updated(LocalDateTime.of(2022, 7, 29, 12, 31))
        .build();
    Mockito.when(versionManagementService.getMasterInfo()).thenReturn(expectedChangeInfo);

    mockMvc.perform(get(BASE_URL))
        .andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.id", is("1")),
            jsonPath("$.name", is("name")),
            jsonPath("$.description", is("description")),
            jsonPath("$.author", is("owner@epam.com")),
            jsonPath("$.latestUpdate", is("2022-07-29T12:31:00.000Z")),
            jsonPath("$.published", nullValue()),
            jsonPath("$.inspector", nullValue()),
            jsonPath("$.validations", nullValue()));
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
            jsonPath("$.validations", nullValue()));
  }
}
