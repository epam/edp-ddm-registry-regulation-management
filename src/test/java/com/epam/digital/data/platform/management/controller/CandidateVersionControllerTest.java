package com.epam.digital.data.platform.management.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.model.dto.ChangeInfo;
import com.epam.digital.data.platform.management.model.dto.CreateVersionRequest;
import com.epam.digital.data.platform.management.service.impl.VersionManagementServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

  ObjectMapper objectMapper = new ObjectMapper();

  @MockBean
  VersionManagementServiceImpl service;

  @Autowired
  MockMvc mockMvc;

  @Test
  @SneakyThrows
  void getVersionListTest() {
    List<ChangeInfo> changeListInfo = new ArrayList<>();
    changeListInfo.add(ChangeInfo.builder().number(1).build());
    Mockito.when(service.getVersionsList()).thenReturn(changeListInfo);
    mockMvc.perform(
            get("/versions/candidates"))
            .andExpectAll(
                    status().isOk(),
                    content().contentType(MediaType.APPLICATION_JSON),
                    jsonPath("$.[0].id", is("1")));
  }

  @Test
  @SneakyThrows
  void createNewVersionTest() {
    var request = new CreateVersionRequest();
    request.setName("versionName");
    request.setDescription("description");

    Mockito.when(service.createNewVersion(request)).thenReturn("1");
    Mockito.when(service.getVersionDetails("1")).thenReturn(
        ChangeInfo.builder()
            .created(LocalDateTime.now())
            .updated(LocalDateTime.now())
            .number(1)
            .build());

    mockMvc.perform(post("/versions/candidates")
            .content(objectMapper.writeValueAsString(request))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpectAll(
            status().isCreated(),
            header().string(HttpHeaders.LOCATION, "/versions/candidates/1"),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.id", is("1")));
  }

  @Test
  @SneakyThrows
  void getVersionDetailsTest() {
    Mockito.when(service.getVersionDetails("1")).thenReturn(
            ChangeInfo.builder()
                    .created(LocalDateTime.now())
                    .updated(LocalDateTime.now())
                    .number(1)
                    .build());
    mockMvc.perform(get("/versions/candidates/1"))
            .andExpectAll(
                    status().isOk(),
                    content().contentType(MediaType.APPLICATION_JSON),
                    jsonPath("$.id", is("1")));
  }

}
