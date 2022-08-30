package com.epam.digital.data.platform.management.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.model.dto.FileStatus;
import com.epam.digital.data.platform.management.model.dto.FormResponse;
import com.epam.digital.data.platform.management.service.FormService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ControllerTest(MasterVersionFormsController.class)
class MasterVersionFormsControllerTest {

  static final String BASE_URL = "/versions/master/forms";
  static final String HEAD_BRANCH = "master";
  static final String FORM_CONTENT = "{\"name\":\"form1\",\"title\":\"form title\"}";

  @Autowired
  MockMvc mockMvc;

  @MockBean
  FormService formService;
  @MockBean
  GerritPropertiesConfig gerritPropertiesConfig;

  @BeforeEach
  void setUp() {
    Mockito.lenient().when(gerritPropertiesConfig.getHeadBranch()).thenReturn(HEAD_BRANCH);
  }

  @Test
  @SneakyThrows
  void getFormsFromMaster() {
    var expectedFormResponse = FormResponse.builder()
        .name("form1")
        .path("forms/form1.json")
        .title("form")
        .status(FileStatus.CURRENT)
        .created(LocalDateTime.of(2022, 7, 29, 15, 6))
        .updated(LocalDateTime.of(2022, 7, 29, 15, 7))
        .build();
    Mockito.when(formService.getFormListByVersion(HEAD_BRANCH))
        .thenReturn(List.of(expectedFormResponse));

    mockMvc.perform(get(BASE_URL))
        .andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.[0].name", is("form1")),
            jsonPath("$.[0].title", is("form")),
            jsonPath("$.[0].created", is("2022-07-29T15:06:00.000Z")),
            jsonPath("$.[0].updated", is("2022-07-29T15:07:00.000Z")));
  }

  @Test
  @SneakyThrows
  void getFormFromMaster() {
    var formName = "form1";
    Mockito.when(formService.getFormContent(formName, HEAD_BRANCH)).thenReturn(FORM_CONTENT);

    mockMvc.perform(get(String.format("%s/%s", BASE_URL, formName)))
        .andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            content().json(FORM_CONTENT));
  }
}
