package com.epam.digital.data.platform.management.controller;

import static org.hamcrest.Matchers.is;
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
    var changeInfo = ChangeInfo.builder()
        .number(1)
        .owner("owner@epam.com")
        .description("description")
        .subject("name")
        .updated(LocalDateTime.of(2022, 7, 29, 12, 31))
        .build();
    Mockito.when(versionManagementService.getMasterInfo()).thenReturn(changeInfo);

    mockMvc.perform(get(BASE_URL))
        .andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.id", is(String.valueOf(changeInfo.getNumber()))),
            jsonPath("$.name", is(String.valueOf(changeInfo.getSubject()))),
            jsonPath("$.description", is(String.valueOf(changeInfo.getDescription()))),
            jsonPath("$.author", is(String.valueOf(changeInfo.getOwner()))),
            jsonPath("$.latestUpdate", is("2022-07-29T12:31:00.000Z")));
  }
}
