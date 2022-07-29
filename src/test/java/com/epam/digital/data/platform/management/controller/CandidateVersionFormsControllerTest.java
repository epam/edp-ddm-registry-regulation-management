package com.epam.digital.data.platform.management.controller;

import com.epam.digital.data.platform.management.model.dto.FileResponse;
import com.epam.digital.data.platform.management.service.impl.FormServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerTest(CandidateVersionFormsController.class)
public class CandidateVersionFormsControllerTest {

  static final String BASE_URL = "/versions/candidates/1/forms";

  ObjectMapper objectMapper = new ObjectMapper();

  @MockBean
  FormServiceImpl formService;

  @Autowired
  MockMvc mockMvc;

  @Test
  @SneakyThrows
  void getFormsByVersionIdTest() {
    var fileResponse = FileResponse.builder().name("formName").path("/").build();
    List<FileResponse> listFiles = new ArrayList<>();
    listFiles.add(fileResponse);

    Mockito.when(formService.getFormListByVersion("1")).thenReturn(listFiles);

    mockMvc.perform(get(BASE_URL))
            .andExpectAll(
                    status().isOk(),
                    content().contentType(MediaType.APPLICATION_JSON),
                    jsonPath("$.[0].name", is(String.valueOf(fileResponse.getName()))));
  }

  @Test
  @SneakyThrows
  void formCreateTest() {
    String jsonForm = "{\"form\":{\"name\":\"formName\"}}";
    mockMvc.perform(post(BASE_URL+"/formName")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(jsonForm)))
            .andExpect(status().isOk());
    Mockito.verify(formService, Mockito.times(1)).createForm(anyString(), anyString(), anyString());
  }

  @Test
  @SneakyThrows
  void getFormTest() {
    String formContent = "{\"form\":{\"name\":\"formName\"}}";
    Mockito.when(formService.getFormContent(anyString(), anyString())).thenReturn(formContent);
    mockMvc.perform(get(BASE_URL+"/formName"))
            .andExpectAll(status().isOk(),
                    jsonPath("$.form.name", is("formName")));
  }

  @Test
  @SneakyThrows
  void updateFormTest() {
    String jsonForm = "{\"form\":{\"name\":\"formName\"}}";
    mockMvc.perform(put(BASE_URL+"/formName")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(jsonForm)))
            .andExpect(status().isOk());
    Mockito.verify(formService, Mockito.times(1)).updateForm(anyString(), anyString(), anyString());
  }

  @Test
  @SneakyThrows
  void deleteFormTest() {
    mockMvc.perform(delete(BASE_URL+"/formName"))
            .andExpect(status().isOk());
    Mockito.verify(formService, Mockito.times(1)).deleteForm(anyString(), anyString());
  }

  @Test
  @SneakyThrows
  void downloadFormTest() {
    String jsonForm = "{\"form\":{\"name\":\"formName\"}}";

    Mockito.when(formService.getFormContent(anyString(), anyString())).thenReturn(jsonForm);

    mockMvc.perform(get(BASE_URL+"/formName/download"))
            .andExpectAll(
                    status().isOk(),
                    content().contentType(MediaType.APPLICATION_JSON_VALUE));

    Mockito.verify(formService, Mockito.times(1)).getFormContent(anyString(), anyString());
  }
}
