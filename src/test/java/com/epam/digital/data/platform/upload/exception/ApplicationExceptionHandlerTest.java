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

package com.epam.digital.data.platform.upload.exception;


import com.epam.digital.data.platform.starter.localization.MessageResolver;
import com.epam.digital.data.platform.upload.controller.UserImportController;
import com.epam.digital.data.platform.upload.i18n.FileValidatorErrorMessageTitle;
import com.epam.digital.data.platform.upload.model.SecurityContext;
import com.epam.digital.data.platform.upload.service.OpenShiftService;
import com.epam.digital.data.platform.upload.service.UserImportService;
import com.epam.digital.data.platform.upload.validator.Validator;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(properties = "spring.cloud.vault.enabled=false")
@ContextConfiguration(
        classes = {UserImportController.class, ApplicationExceptionHandler.class}
)
@AutoConfigureMockMvc(addFilters = false)
class ApplicationExceptionHandlerTest {

  static final String BASE_URL = "/batch-loads/users";
  static MockMultipartFile file = new MockMultipartFile("file", "users.csv", MediaType.MULTIPART_FORM_DATA_VALUE, "test".getBytes());

  @Autowired
  MockMvc mockMvc;

  @MockBean
  UserImportService userImportService;

  @MockBean
  OpenShiftService openShiftService;

  @MockBean
  Validator validator;

  @MockBean
  private MessageResolver messageResolver;

  @Test
  @SneakyThrows
  void shouldReturnRuntimeErrorOnGenericException() {
    when(userImportService.storeFile(file, new SecurityContext())).thenThrow(RuntimeException.class);

    mockMvc.perform(multipart(BASE_URL).file(file))
            .andExpect(status().isInternalServerError())
            .andExpectAll(
                    jsonPath("$.code").value(is("RUNTIME_ERROR")),
                    jsonPath("$.statusDetails").doesNotExist());
  }

  @Test
  @SneakyThrows
  void shouldReturnInternalErrorUploadProcessingException() {
    when(userImportService.storeFile(file, new SecurityContext())).thenThrow(new FileLoadProcessingException("ERROR", new RuntimeException()));

    mockMvc.perform(multipart(BASE_URL).file(file))
            .andExpect(status().isBadRequest())
            .andExpect(response -> assertTrue(
                    response.getResolvedException() instanceof FileLoadProcessingException))
            .andExpect(
                    jsonPath("$.code").value(is("IMPORT_CEPH_ERROR"))
            );
  }

  @Test
  @SneakyThrows
  void shouldReturnDeleteProcessingException() {
    String id = UUID.randomUUID().toString();
    doThrow(new CephInvocationException("ERROR", new RuntimeException())).when(userImportService).delete(id);

    mockMvc.perform(delete(BASE_URL + "/{id}", id))
            .andExpect(status().isInternalServerError())
            .andExpect(response -> assertTrue(
                    response.getResolvedException() instanceof CephInvocationException))
            .andExpect(
                    jsonPath("$.code").value(is("RUNTIME_ERROR"))
            );
  }

  @Test
  @SneakyThrows
  void shouldReturnGetProcessingException() {
    when(userImportService.getFileInfo(any())).thenThrow(new GetProcessingException("ERROR", new RuntimeException()));

    mockMvc.perform(get(BASE_URL))
            .andExpect(status().isNotFound())
            .andExpect(response -> assertTrue(
                    response.getResolvedException() instanceof GetProcessingException))
            .andExpect(
                    jsonPath("$.code").value(is("GET_CEPH_ERROR"))
            );
  }

  @Test
  @SneakyThrows
  void shouldReturn403WhenForbiddenOperation() {
    when(userImportService.storeFile(file, new SecurityContext())).thenThrow(AccessDeniedException.class);

    mockMvc.perform(multipart(BASE_URL).file(file))
            .andExpectAll(
                    status().isForbidden(),
                    jsonPath("$.code").value(is("FORBIDDEN_OPERATION")));
  }

  @Test
  @SneakyThrows
  void shouldReturnOpenShiftInvocationException() {
    doThrow(new OpenShiftInvocationException("ERROR", new RuntimeException())).when(openShiftService).startImport(new SecurityContext());

    mockMvc.perform(post(BASE_URL + "/imports"))
            .andExpect(status().isInternalServerError())
            .andExpect(response -> assertTrue(
                    response.getResolvedException() instanceof OpenShiftInvocationException))
            .andExpect(
                    jsonPath("$.code").value(is("RUNTIME_ERROR"))
            );
  }

  @Test
  @SneakyThrows
  void shouldThrowJwtParsingException() {
    when(userImportService.storeFile(file, new SecurityContext())).thenThrow(JwtParsingException.class);

    mockMvc.perform(multipart(BASE_URL).file(file))
            .andExpectAll(
                    status().isBadRequest(),
                    jsonPath("$.code").value(is("JWT_PARSING_ERROR")));
  }

  @Test
  @SneakyThrows
  void shouldThrowVaultInvocationException() {
    when(userImportService.storeFile(file, new SecurityContext())).thenThrow(VaultInvocationException.class);

    mockMvc.perform(multipart(BASE_URL).file(file))
            .andExpectAll(
                    status().isInternalServerError(),
                    jsonPath("$.code").value(is("RUNTIME_ERROR")));
  }

  @Test
  @SneakyThrows
  void shouldThrowMaxUploadSizeExceededException() {
    when(userImportService.storeFile(file, new SecurityContext())).thenThrow(MaxUploadSizeExceededException.class);
    when(messageResolver.getMessage(FileValidatorErrorMessageTitle.SIZE)).thenReturn("Файл занадто великого розміру.");

    mockMvc.perform(multipart(BASE_URL).file(file))
            .andExpectAll(
                    status().isBadRequest(),
                    jsonPath("$.code").value(is("FILE_SIZE_ERROR")),
                    jsonPath("$.localizedMessage").value(is("Файл занадто великого розміру.")));
  }

  @Test
  @SneakyThrows
  void shouldThrowFileEncodingException() {
    when(userImportService.storeFile(file, new SecurityContext())).thenThrow(FileEncodingException.class);
    when(messageResolver.getMessage(FileValidatorErrorMessageTitle.ENCODING)).thenReturn("Файл невідповідного кодування.");

    mockMvc.perform(multipart(BASE_URL).file(file))
            .andExpectAll(
                    status().isBadRequest(),
                    jsonPath("$.code").value(is("FILE_ENCODING_EXCEPTION")),
                    jsonPath("$.localizedMessage").value(is("Файл невідповідного кодування.")));
  }

  @Test
  @SneakyThrows
  void shouldThrowFileExtensionException() {
    when(userImportService.storeFile(file, new SecurityContext())).thenThrow(FileExtensionException.class);
    when(messageResolver.getMessage(FileValidatorErrorMessageTitle.EXTENSION)).thenReturn("Невідповідний формат файлу.");

    mockMvc.perform(multipart(BASE_URL).file(file))
            .andExpectAll(
                    status().isBadRequest(),
                    jsonPath("$.code").value(is("FILE_EXTENSION_ERROR")),
                    jsonPath("$.localizedMessage").value(is("Невідповідний формат файлу.")));
  }
}