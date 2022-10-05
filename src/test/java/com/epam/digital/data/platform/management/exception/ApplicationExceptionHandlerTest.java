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

package com.epam.digital.data.platform.management.exception;


import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.controller.CandidateVersionBusinessProcessesController;
import com.epam.digital.data.platform.management.controller.CandidateVersionFormsController;
import com.epam.digital.data.platform.management.controller.MasterVersionFormsController;
import com.epam.digital.data.platform.management.controller.UserImportController;
import com.epam.digital.data.platform.management.i18n.FileValidatorErrorMessageTitle;
import com.epam.digital.data.platform.management.model.SecurityContext;
import com.epam.digital.data.platform.management.service.BusinessProcessService;
import com.epam.digital.data.platform.management.service.FormService;
import com.epam.digital.data.platform.management.service.OpenShiftService;
import com.epam.digital.data.platform.management.service.impl.UserImportServiceImpl;
import com.epam.digital.data.platform.management.validator.Validator;
import com.epam.digital.data.platform.starter.localization.MessageResolver;
import java.util.UUID;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
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

@WebMvcTest(properties = "spring.cloud.vault.enabled=false")
@ContextConfiguration(
    classes = {MasterVersionFormsController.class, UserImportController.class,
        ApplicationExceptionHandler.class, CandidateVersionBusinessProcessesController.class,
        CandidateVersionFormsController.class}
)
@AutoConfigureMockMvc(addFilters = false)
class ApplicationExceptionHandlerTest {

  static final String BASE_URL = "/batch-loads/users";
  static MockMultipartFile file = new MockMultipartFile("file", "users.csv",
      MediaType.MULTIPART_FORM_DATA_VALUE, "test".getBytes());

  @Autowired
  MockMvc mockMvc;

  @MockBean
  UserImportServiceImpl userImportService;

  @MockBean
  FormService formService;
  @MockBean
  BusinessProcessService businessProcessService;
  @MockBean
  GerritPropertiesConfig gerritPropertiesConfig;

  @MockBean
  OpenShiftService openShiftService;

  @MockBean
  Validator validator;

  @MockBean
  private MessageResolver messageResolver;

  @Test
  @SneakyThrows
  void shouldReturnRuntimeErrorOnGenericException() {
    when(userImportService.storeFile(file, new SecurityContext()))
        .thenThrow(RuntimeException.class);

    mockMvc.perform(multipart(BASE_URL).file(file))
        .andExpect(status().isInternalServerError())
        .andExpectAll(
            jsonPath("$.code").value(is("RUNTIME_ERROR")),
            jsonPath("$.statusDetails").doesNotExist());
  }

  @Test
  @SneakyThrows
  void shouldReturnInternalErrorUploadProcessingException() {
    when(userImportService.storeFile(file, new SecurityContext()))
        .thenThrow(new FileLoadProcessingException("ERROR", new RuntimeException()));

    mockMvc.perform(multipart(BASE_URL).file(file))
        .andExpectAll(
            status().isBadRequest(),
            response -> Assertions.assertThat(response.getResolvedException())
                .isInstanceOf(FileLoadProcessingException.class),
            jsonPath("$.code").value(is("IMPORT_CEPH_ERROR")));
  }

  @Test
  @SneakyThrows
  void shouldReturnDeleteProcessingException() {
    String id = UUID.randomUUID().toString();
    doThrow(new CephInvocationException("ERROR", new RuntimeException()))
        .when(userImportService).delete(id);

    mockMvc.perform(delete(BASE_URL + "/{id}", id))
        .andExpectAll(
            status().isInternalServerError(),
            response -> Assertions.assertThat(response.getResolvedException())
                .isInstanceOf(CephInvocationException.class),
            jsonPath("$.code").value(is("RUNTIME_ERROR"))
        );
  }

  @Test
  @SneakyThrows
  void shouldReturnGetProcessingException() {
    when(userImportService.getFileInfo(any())).thenThrow(
        new GetProcessingException("ERROR", new RuntimeException()));

    mockMvc.perform(get(BASE_URL))
        .andExpectAll(
            status().isNotFound(),
            response -> Assertions.assertThat(response.getResolvedException())
                .isInstanceOf(GetProcessingException.class),
            jsonPath("$.code").value(is("GET_CEPH_ERROR"))
        );
  }

  @Test
  @SneakyThrows
  void shouldReturn403WhenForbiddenOperation() {
    when(userImportService.storeFile(file, new SecurityContext())).thenThrow(
        AccessDeniedException.class);

    mockMvc.perform(multipart(BASE_URL).file(file))
        .andExpectAll(
            status().isForbidden(),
            jsonPath("$.code").value(is("FORBIDDEN_OPERATION")));
  }

  @Test
  @SneakyThrows
  void shouldReturnOpenShiftInvocationException() {
    doThrow(new OpenShiftInvocationException("ERROR", new RuntimeException()))
        .when(openShiftService).startImport(new SecurityContext());

    mockMvc.perform(post(BASE_URL + "/imports"))
        .andExpectAll(
            status().isInternalServerError(),
            response -> Assertions.assertThat(response.getResolvedException())
                .isInstanceOf(OpenShiftInvocationException.class),
            jsonPath("$.code").value(is("RUNTIME_ERROR"))
        );
  }

  @Test
  @SneakyThrows
  void shouldThrowJwtParsingException() {
    when(userImportService.storeFile(file, new SecurityContext()))
        .thenThrow(JwtParsingException.class);

    mockMvc.perform(multipart(BASE_URL).file(file))
        .andExpectAll(
            status().isBadRequest(),
            jsonPath("$.code").value(is("JWT_PARSING_ERROR")));
  }

  @Test
  @SneakyThrows
  void shouldThrowVaultInvocationException() {
    when(userImportService.storeFile(file, new SecurityContext()))
        .thenThrow(VaultInvocationException.class);

    mockMvc.perform(multipart(BASE_URL).file(file))
        .andExpectAll(
            status().isInternalServerError(),
            jsonPath("$.code").value(is("RUNTIME_ERROR")));
  }

  @Test
  @SneakyThrows
  void shouldThrowMaxUploadSizeExceededException() {
    when(userImportService.storeFile(file, new SecurityContext()))
        .thenThrow(MaxUploadSizeExceededException.class);
    when(messageResolver.getMessage(FileValidatorErrorMessageTitle.SIZE)).thenReturn(
        "Файл занадто великого розміру.");

    mockMvc.perform(multipart(BASE_URL).file(file))
        .andExpectAll(
            status().isBadRequest(),
            jsonPath("$.code").value(is("FILE_SIZE_ERROR")),
            jsonPath("$.localizedMessage").value(is("Файл занадто великого розміру.")));
  }

  @Test
  @SneakyThrows
  void shouldThrowFileEncodingException() {
    when(userImportService.storeFile(file, new SecurityContext()))
        .thenThrow(FileEncodingException.class);
    when(messageResolver.getMessage(FileValidatorErrorMessageTitle.ENCODING)).thenReturn(
        "Файл невідповідного кодування.");

    mockMvc.perform(multipart(BASE_URL).file(file))
        .andExpectAll(
            status().isBadRequest(),
            jsonPath("$.code").value(is("FILE_ENCODING_EXCEPTION")),
            jsonPath("$.localizedMessage").value(is("Файл невідповідного кодування.")));
  }

  @Test
  @SneakyThrows
  void shouldThrowFileExtensionException() {
    when(userImportService.storeFile(file, new SecurityContext()))
        .thenThrow(FileExtensionException.class);
    when(messageResolver.getMessage(FileValidatorErrorMessageTitle.EXTENSION)).thenReturn(
        "Невідповідний формат файлу.");

    mockMvc.perform(multipart(BASE_URL).file(file))
        .andExpectAll(
            status().isBadRequest(),
            jsonPath("$.code").value(is("FILE_EXTENSION_ERROR")),
            jsonPath("$.localizedMessage").value(is("Невідповідний формат файлу.")));
  }

  @Test
  @SneakyThrows
  void shouldReturnRuntimeErrorOnReadingRepositoryException() {
    var repoName = RandomString.make();
    when(gerritPropertiesConfig.getHeadBranch()).thenReturn(repoName);
    when(formService.getFormListByVersion(repoName))
        .thenThrow(ReadingRepositoryException.class);

    mockMvc.perform(get("/versions/master/forms"))
        .andExpect(status().isInternalServerError())
        .andExpectAll(
            jsonPath("$.code").value(is("READING_REPOSITORY_EXCEPTION")),
            jsonPath("$.statusDetails").doesNotExist());
  }

  @Test
  @SneakyThrows
  void shouldThrowFormAlreadyExistsException() {
    var formName = RandomString.make();
    var versionName = RandomString.make();
    var content = RandomString.make();
    doThrow(FormAlreadyExistsException.class).when(formService).createForm(formName, content, versionName);
    when(messageResolver.getMessage(FileValidatorErrorMessageTitle.FORM_ALREADY_EXISTS)).thenReturn(
        "Неунікальна службова назва форми");

    mockMvc.perform(post("/versions/candidates/{versionCandidateId}/forms/{formName}", versionName, formName).content(content))
        .andExpect(status().isConflict())
        .andExpectAll(
            jsonPath("$.code").value("FORM_ALREADY_EXISTS_EXCEPTION"),
            jsonPath("$.statusDetails").doesNotExist(),
            jsonPath("$.localizedMessage").value(is("Неунікальна службова назва форми")));
  }

  @Test
  @SneakyThrows
  void shouldThrowBusinessProcessAlreadyExistsException() {
    var bpName = RandomString.make();
    var versionName = RandomString.make();
    var content = RandomString.make();
    doThrow(BusinessProcessAlreadyExists.class).when(businessProcessService).createProcess(bpName, content, versionName);

    when(messageResolver.getMessage(FileValidatorErrorMessageTitle.BUSINESS_PROCESS_ALREADY_EXISTS)).thenReturn(
        "Бізнес-процес з такою службовою назвою вже існує");
    mockMvc.perform(post("/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}", versionName, bpName).content(content))
        .andExpect(status().isConflict())
        .andExpectAll(
            jsonPath("$.code").value("BUSINESS_PROCESS_ALREADY_EXISTS_EXCEPTION"),
            jsonPath("$.statusDetails").doesNotExist(),
            jsonPath("$.localizedMessage").value(is("Бізнес-процес з такою службовою назвою вже існує")));
  }
}