package com.epam.digital.data.platform.management.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.model.dto.FileStatus;
import com.epam.digital.data.platform.management.model.dto.FormResponse;
import com.epam.digital.data.platform.management.service.impl.FormServiceImpl;
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

@ControllerTest(CandidateVersionFormsController.class)
class CandidateVersionFormsControllerTest {

  static final String CANDIDATE_VERSION_ID = "1";
  static final String BASE_URL = "/versions/candidates/1/forms";
  static final String FORM_CONTENT = "{\"name\":\"form1\",\"title\":\"form title\"}";

  @Autowired
  MockMvc mockMvc;

  @MockBean
  FormServiceImpl formService;

  @Test
  @SneakyThrows
  void getFormsByVersionIdTest() {
    var fileResponse = FormResponse.builder()
        .name("formName")
        .title("form")
        .path("/")
        .status(FileStatus.CHANGED)
        .created(LocalDateTime.of(2022, 7, 29, 18, 55))
        .updated(LocalDateTime.of(2022, 7, 29, 18, 56))
        .build();
    Mockito.when(formService.getFormListByVersion(CANDIDATE_VERSION_ID))
        .thenReturn(List.of(fileResponse));

    mockMvc.perform(get(BASE_URL))
        .andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.[0].name", is("formName")),
            jsonPath("$.[0].title", is("form")),
            jsonPath("$.[0].created", is("2022-07-29T18:55:00.000Z")),
            jsonPath("$.[0].updated", is("2022-07-29T18:56:00.000Z")));
  }

  @Test
  @SneakyThrows
  void formCreateTest() {
    mockMvc.perform(post(BASE_URL + "/formName")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FORM_CONTENT))
        .andExpectAll(
            status().isCreated(),
            header().string(HttpHeaders.LOCATION, "/versions/candidates/1/forms/formName"),
            content().contentType(MediaType.APPLICATION_JSON),
            content().json(FORM_CONTENT));

    Mockito.verify(formService).createForm("formName", FORM_CONTENT, CANDIDATE_VERSION_ID);
  }

  @Test
  @SneakyThrows
  void getFormTest() {
    Mockito.when(formService.getFormContent("formName", CANDIDATE_VERSION_ID))
        .thenReturn(FORM_CONTENT);

    mockMvc.perform(get(BASE_URL + "/formName"))
        .andExpectAll(
            status().isOk(),
            content().json(FORM_CONTENT));
  }

  @Test
  @SneakyThrows
  void updateFormTest() {
    mockMvc.perform(put(BASE_URL + "/formName")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FORM_CONTENT))
        .andExpectAll(
            status().isOk(),
            content().json(FORM_CONTENT));

    Mockito.verify(formService).updateForm(FORM_CONTENT, "formName", CANDIDATE_VERSION_ID);
  }

  @Test
  @SneakyThrows
  void deleteFormTest() {
    mockMvc.perform(delete(BASE_URL + "/formName"))
        .andExpect(status().isNoContent());

    Mockito.verify(formService).deleteForm("formName", CANDIDATE_VERSION_ID);
  }
}
