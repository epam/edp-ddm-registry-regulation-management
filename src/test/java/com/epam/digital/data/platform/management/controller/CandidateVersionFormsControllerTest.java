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

  static final String BASE_URL = "/versions/candidates/1/forms";

  @MockBean
  FormServiceImpl formService;

  @Autowired
  MockMvc mockMvc;

  @Test
  @SneakyThrows
  void getFormsByVersionIdTest() {
    var fileResponse = FormResponse
        .builder()
        .name("formName")
        .title("form")
        .path("/")
        .status(FileStatus.CHANGED)
        .created(LocalDateTime.of(2022, 7, 29, 18, 55))
        .updated(LocalDateTime.of(2022, 7, 29, 18, 56))
        .build();
    Mockito.when(formService.getFormListByVersion("1")).thenReturn(List.of(fileResponse));

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
    String jsonForm = "{\"form\":{\"name\":\"formName\"}}";

    mockMvc.perform(post(BASE_URL + "/formName")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonForm))
        .andExpectAll(
            status().isCreated(),
            header().string(HttpHeaders.LOCATION, "/versions/candidates/1/forms/formName"),
            content().contentType(MediaType.APPLICATION_JSON),
            content().json(jsonForm));

    Mockito.verify(formService).createForm("formName", jsonForm, "1");
  }

  @Test
  @SneakyThrows
  void getFormTest() {
    String formContent = "{\"form\":{\"name\":\"formName\"}}";
    Mockito.when(formService.getFormContent("formName", "1")).thenReturn(formContent);

    mockMvc.perform(get(BASE_URL + "/formName"))
        .andExpectAll(
            status().isOk(),
            content().json(formContent));
  }

  @Test
  @SneakyThrows
  void updateFormTest() {
    String jsonForm = "{\"form\":{\"name\":\"formName\"}}";

    mockMvc.perform(put(BASE_URL + "/formName")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonForm))
        .andExpectAll(
            status().isOk(),
            content().json(jsonForm));

    Mockito.verify(formService).updateForm(jsonForm, "formName", "1");
  }

  @Test
  @SneakyThrows
  void deleteFormTest() {
    mockMvc.perform(delete(BASE_URL + "/formName"))
        .andExpect(status().isNoContent());
    Mockito.verify(formService).deleteForm("formName", "1");
  }

  @Test
  @SneakyThrows
  void downloadFormTest() {
    String jsonForm = "{\"form\":{\"name\":\"formName\"}}";

    Mockito.when(formService.getFormContent("formName", "1")).thenReturn(jsonForm);

    mockMvc.perform(get(BASE_URL + "/formName/download"))
        .andExpectAll(
            status().isOk(),
            header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=formName.json"),
            content().contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE),
            content().json(jsonForm));

    Mockito.verify(formService).getFormContent("formName", "1");
  }
}
