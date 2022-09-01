package com.epam.digital.data.platform.management.controller;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.model.dto.ChangeInfoDetailedDto;
import com.epam.digital.data.platform.management.model.dto.CreateVersionRequest;
import com.epam.digital.data.platform.management.service.impl.VersionManagementServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ControllerTest(CandidateVersionController.class)
class CandidateVersionControllerTest {

  static final String BASE_URL = "/versions/candidates";

  @Autowired
  MockMvc mockMvc;

  @MockBean
  VersionManagementServiceImpl service;

  ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @SneakyThrows
  void getVersionListTest() {
    var changeInfo = ChangeInfoDetailedDto.builder()
        .number(1)
        .subject("subject")
        .description("description")
        .build();
    Mockito.when(service.getVersionsList()).thenReturn(List.of(changeInfo));

    mockMvc.perform(get(BASE_URL))
        .andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.[0].id", is("1")),
            jsonPath("$.[0].name", is("subject")),
            jsonPath("$.[0].description", is("description")));
  }

  @Test
  @SneakyThrows
  void createNewVersionTest() {
    var request = new CreateVersionRequest();
    request.setName("versionName");
    request.setDescription("description");
    Mockito.when(service.createNewVersion(request)).thenReturn("1");

    var expectedVersionDetails = ChangeInfoDetailedDto.builder()
        .number(1)
        .subject("versionName")
        .description("description")
        .owner("author")
        .created(LocalDateTime.of(2022, 8, 10, 11, 30))
        .updated(LocalDateTime.of(2022, 8, 10, 11, 40))
        .mergeable(true)
        .build();
    Mockito.when(service.getVersionDetails("1")).thenReturn(expectedVersionDetails);

    mockMvc.perform(post(BASE_URL)
            .content(objectMapper.writeValueAsString(request))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpectAll(
            status().isCreated(),
            header().string(HttpHeaders.LOCATION, "/versions/candidates/1"),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.id", is("1")),
            jsonPath("$.name", is("versionName")),
            jsonPath("$.description", is("description")),
            jsonPath("$.author", is("author")),
            jsonPath("$.creationDate", is("2022-08-10T11:30:00.000Z")),
            jsonPath("$.latestUpdate", is("2022-08-10T11:40:00.000Z")),
            jsonPath("$.hasConflicts", is(false)),
            jsonPath("$.inspections", nullValue()),
            jsonPath("$.validations", nullValue()));
  }

  @Test
  @SneakyThrows
  void getVersionDetailsTest() {
    var expectedVersionDetails = ChangeInfoDetailedDto.builder()
        .number(1)
        .subject("versionName")
        .description("description")
        .owner("author")
        .created(LocalDateTime.of(2022, 8, 10, 11, 30))
        .updated(LocalDateTime.of(2022, 8, 10, 11, 40))
        .mergeable(true)
        .build();
    Mockito.when(service.getVersionDetails("1")).thenReturn(expectedVersionDetails);

    mockMvc.perform(get(String.format("%s/%s", BASE_URL, "1")))
        .andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.id", is("1")),
            jsonPath("$.name", is("versionName")),
            jsonPath("$.description", is("description")),
            jsonPath("$.author", is("author")),
            jsonPath("$.creationDate", is("2022-08-10T11:30:00.000Z")),
            jsonPath("$.latestUpdate", is("2022-08-10T11:40:00.000Z")),
            jsonPath("$.hasConflicts", is(false)),
            jsonPath("$.inspections", nullValue()),
            jsonPath("$.validations", nullValue()));
  }
}
