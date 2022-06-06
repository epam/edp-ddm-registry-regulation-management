/*
 * Copyright 2023 EPAM Systems.
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

package com.epam.digital.data.platform.management.restapi.exception;


import static com.epam.digital.data.platform.management.restapi.exception.ApplicationExceptionHandler.GROUPS_FIELD_REQUIRED_EXCEPTION;
import static com.epam.digital.data.platform.management.restapi.exception.ApplicationExceptionHandler.GROUPS_NAME_REGEX_EXCEPTION;
import static com.epam.digital.data.platform.management.restapi.exception.ApplicationExceptionHandler.GROUPS_NAME_REQUIRED_EXCEPTION;
import static com.epam.digital.data.platform.management.restapi.exception.ApplicationExceptionHandler.GROUPS_NAME_UNIQUE_EXCEPTION;
import static com.epam.digital.data.platform.management.restapi.exception.ApplicationExceptionHandler.GROUPS_PROCESS_DEFINITION_DUPLICATES_EXCEPTION;
import static com.epam.digital.data.platform.management.restapi.exception.ApplicationExceptionHandler.GROUPS_PROCESS_DEFINITION_EXCEPTION;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.exception.BusinessProcessAlreadyExistsException;
import com.epam.digital.data.platform.management.forms.exception.FormAlreadyExistsException;
import com.epam.digital.data.platform.management.forms.service.FormService;
import com.epam.digital.data.platform.management.gerritintegration.model.CreateChangeInputDto;
import com.epam.digital.data.platform.management.gitintegration.exception.GitFileNotFoundException;
import com.epam.digital.data.platform.management.groups.exception.GroupDuplicateProcessDefinitionException;
import com.epam.digital.data.platform.management.groups.exception.GroupEmptyProcessDefinitionException;
import com.epam.digital.data.platform.management.groups.exception.GroupNameRegexException;
import com.epam.digital.data.platform.management.groups.exception.GroupNameRequiredException;
import com.epam.digital.data.platform.management.groups.exception.GroupNameUniqueException;
import com.epam.digital.data.platform.management.groups.exception.GroupsParseException;
import com.epam.digital.data.platform.management.groups.exception.GroupsRequiredException;
import com.epam.digital.data.platform.management.groups.model.GroupListDetails;
import com.epam.digital.data.platform.management.groups.service.GroupService;
import com.epam.digital.data.platform.management.groups.validation.BpGroupingValidator;
import com.epam.digital.data.platform.management.osintegration.exception.GetProcessingException;
import com.epam.digital.data.platform.management.osintegration.exception.OpenShiftInvocationException;
import com.epam.digital.data.platform.management.osintegration.service.OpenShiftService;
import com.epam.digital.data.platform.management.restapi.controller.CandidateVersionBusinessProcessesController;
import com.epam.digital.data.platform.management.restapi.controller.CandidateVersionController;
import com.epam.digital.data.platform.management.restapi.controller.CandidateVersionFormsController;
import com.epam.digital.data.platform.management.restapi.controller.CandidateVersionGroupsController;
import com.epam.digital.data.platform.management.restapi.controller.MasterVersionFormsController;
import com.epam.digital.data.platform.management.restapi.controller.MasterVersionGroupsController;
import com.epam.digital.data.platform.management.restapi.controller.UserImportController;
import com.epam.digital.data.platform.management.restapi.i18n.FileValidatorErrorMessageTitle;
import com.epam.digital.data.platform.management.restapi.mapper.ControllerMapper;
import com.epam.digital.data.platform.management.restapi.model.CreateVersionRequest;
import com.epam.digital.data.platform.management.restapi.service.BuildStatusService;
import com.epam.digital.data.platform.management.restapi.util.TestUtils;
import com.epam.digital.data.platform.management.security.model.SecurityContext;
import com.epam.digital.data.platform.management.service.BusinessProcessService;
import com.epam.digital.data.platform.management.users.exception.CephInvocationException;
import com.epam.digital.data.platform.management.users.exception.FileEncodingException;
import com.epam.digital.data.platform.management.users.exception.FileExtensionException;
import com.epam.digital.data.platform.management.users.exception.FileLoadProcessingException;
import com.epam.digital.data.platform.management.users.exception.JwtParsingException;
import com.epam.digital.data.platform.management.users.exception.VaultInvocationException;
import com.epam.digital.data.platform.management.users.model.CephFileInfoDto;
import com.epam.digital.data.platform.management.users.service.UserImportServiceImpl;
import com.epam.digital.data.platform.management.versionmanagement.service.VersionManagementService;
import com.epam.digital.data.platform.starter.localization.MessageResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import javax.validation.ConstraintViolationException;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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

@WebMvcTest(properties = {"spring.cloud.vault.enabled=false", "spring.cloud.kubernetes.enabled=false"})
@ContextConfiguration(
    classes = {MasterVersionFormsController.class, UserImportController.class,
        ApplicationExceptionHandler.class, CandidateVersionBusinessProcessesController.class,
        CandidateVersionFormsController.class, CandidateVersionController.class,
        MasterVersionGroupsController.class, CandidateVersionGroupsController.class}
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
  BuildStatusService buildStatusService;

  @MockBean
  FormService formService;
  @MockBean
  BusinessProcessService businessProcessService;
  @MockBean
  GroupService groupService;
  @MockBean
  BpGroupingValidator groupingValidator;
  @MockBean
  GerritPropertiesConfig gerritPropertiesConfig;
  @MockBean
  VersionManagementService versionManagementService;
  @MockBean
  OpenShiftService openShiftService;
  @MockBean
  ControllerMapper mapper;
  @MockBean
  MessageResolver messageResolver;

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
    when(userImportService.getFileInfo(eq(new SecurityContext()))).thenThrow(
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
    doReturn(CephFileInfoDto.builder().id("id").build())
        .when(userImportService).getFileInfo(new SecurityContext());
    doThrow(new OpenShiftInvocationException("ERROR", new RuntimeException()))
        .when(openShiftService).startImport("id", new SecurityContext());

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
  void shouldThrowFormAlreadyExistsException() {
    var formName = RandomString.make();
    var versionName = RandomString.make();
    var content = RandomString.make();
    doThrow(FormAlreadyExistsException.class).when(formService)
        .createForm(formName, content, versionName);
    when(messageResolver.getMessage(FileValidatorErrorMessageTitle.FORM_ALREADY_EXISTS)).thenReturn(
        "Неунікальна службова назва форми");

    mockMvc.perform(post("/versions/candidates/{versionCandidateId}/forms/{formName}", versionName,
            formName).content(content))
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
    var content = TestUtils.getContent("bp-correct.xml");
    doThrow(BusinessProcessAlreadyExistsException.class).when(businessProcessService)
        .createProcess(bpName, content, versionName);

    when(messageResolver.getMessage(
        FileValidatorErrorMessageTitle.BUSINESS_PROCESS_ALREADY_EXISTS)).thenReturn(
        "Бізнес-процес з такою службовою назвою вже існує");
    mockMvc.perform(
            post("/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}",
                versionName, bpName).content(content))
        .andExpect(status().isConflict())
        .andExpectAll(
            jsonPath("$.code").value("BUSINESS_PROCESS_ALREADY_EXISTS_EXCEPTION"),
            jsonPath("$.statusDetails").doesNotExist(),
            jsonPath("$.localizedMessage").value(
                is("Бізнес-процес з такою службовою назвою вже існує")));
  }

  @Test
  @SneakyThrows
  void shouldThrowConstraintViolationExceptionDuringBpValidation() {
    var bpName = RandomString.make();
    var versionName = RandomString.make();
    var content = TestUtils.getContent("bp-incorrect-tag.xml");

    mockMvc.perform(
        post("/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}",
            versionName, bpName).content(content)
    ).andExpectAll(
        status().isUnprocessableEntity(),
        jsonPath("$.code").value("BUSINESS_PROCESS_CONTENT_EXCEPTION"),
        jsonPath("$.statusDetails").doesNotExist(),
        jsonPath("$.localizedMessage").doesNotExist());
  }

  @Test
  @SneakyThrows
  void shouldThrowConstraintViolationExceptionOnInvalidBP() {
    var bpName = RandomString.make();
    var versionName = RandomString.make();
    var content = TestUtils.getContent("bp-correct.xml");
    doThrow(ConstraintViolationException.class).when(businessProcessService)
        .createProcess(bpName, content, versionName);
    mockMvc.perform(
            post("/versions/candidates/{versionCandidateId}/business-processes/{businessProcessName}",
                versionName, bpName).content(content))
        .andExpect(status().isInternalServerError())
        .andExpectAll(
            jsonPath("$.code").value("CONSTRAINT_VIOLATION_EXCEPTION"),
            jsonPath("$.statusDetails").doesNotExist(),
            jsonPath("$.localizedMessage").doesNotExist());
  }

  @Test
  @SneakyThrows
  void shouldThrowConstraintViolationExceptionOnInvalidCandidate() {
    var versionName = RandomString.make();
    var versionDescription = RandomString.make();
    var request = new CreateVersionRequest();
    request.setName(versionName);
    request.setDescription(versionDescription);
    var createChangeInputDto = CreateChangeInputDto.builder()
        .name(versionName)
        .description(versionDescription)
        .build();
    Mockito.when(versionManagementService.createNewVersion(createChangeInputDto)).thenReturn("1");
    Mockito.doReturn(createChangeInputDto).when(mapper).toDto(request);
    doThrow(ConstraintViolationException.class)
        .when(versionManagementService).createNewVersion(createChangeInputDto);
    mockMvc.perform(post("/versions/candidates")
            .contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(request))
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpectAll(
            status().isInternalServerError(),
            jsonPath("$.code").value("CONSTRAINT_VIOLATION_EXCEPTION"),
            jsonPath("$.statusDetails").doesNotExist(),
            jsonPath("$.localizedMessage").doesNotExist());
  }

  @Test
  @SneakyThrows
  void shouldThrowsGroupsParsingException() {
    final var headBranch = RandomString.make();
    Mockito.doReturn(headBranch)
        .when(gerritPropertiesConfig).getHeadBranch();

    doThrow(GroupsParseException.class).when(groupService)
        .getGroupsByVersion(headBranch);

    mockMvc.perform(
        get("/versions/master/business-process-groups")
            .accept(MediaType.APPLICATION_JSON)
    ).andExpectAll(
        status().isInternalServerError(),
        content().contentType(MediaType.APPLICATION_JSON),
        jsonPath("$.code").value("GROUPS_PARSING_EXCEPTION"),
        jsonPath("$.localizedMessage").doesNotExist());

    Mockito.verify(groupService).getGroupsByVersion(headBranch);
  }

  @Test
  @SneakyThrows
  void shouldThrowsGroupsParsingExceptionOnVersionCandidate() {
    final var version = RandomString.make();
    Mockito.doReturn(version)
        .when(gerritPropertiesConfig).getHeadBranch();

    doThrow(GroupsParseException.class).when(groupService)
        .getGroupsByVersion(version);

    mockMvc.perform(
        get("/versions/candidates/{versionCandidateId}/business-process-groups", version)
            .accept(MediaType.APPLICATION_JSON)
    ).andExpectAll(
        status().isInternalServerError(),
        content().contentType(MediaType.APPLICATION_JSON),
        jsonPath("$.code").value("GROUPS_PARSING_EXCEPTION"),
        jsonPath("$.localizedMessage").doesNotExist());

    Mockito.verify(groupService).getGroupsByVersion(version);
  }

  @Test
  @SneakyThrows
  void shouldThrowGroupsRequiredException() {
    final var version = RandomString.make();
    final var grouping = GroupListDetails.builder().build();
    doThrow(new GroupsRequiredException("Groups are mandatory field")).when(groupingValidator)
        .validate(grouping);

    mockMvc.perform(
            post("/versions/candidates/{versionCandidateId}/business-process-groups", version)
                .content(new ObjectMapper().writeValueAsString(grouping))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnprocessableEntity())
        .andExpectAll(
            jsonPath("$.code").value(GROUPS_FIELD_REQUIRED_EXCEPTION),
            jsonPath("$.details").value("Groups are mandatory field")
        );
  }

  @Test
  @SneakyThrows
  void shouldTrowGroupEmptyProcessDefinitionException() {
    final var version = RandomString.make();
    final var grouping = GroupListDetails.builder().build();
    doThrow(new GroupEmptyProcessDefinitionException("Process definition cannot be empty")).when(
            groupingValidator)
        .validate(grouping);

    mockMvc.perform(
            post("/versions/candidates/{versionCandidateId}/business-process-groups", version)
                .content(new ObjectMapper().writeValueAsString(grouping))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnprocessableEntity())
        .andExpectAll(
            jsonPath("$.code").value(GROUPS_PROCESS_DEFINITION_EXCEPTION),
            jsonPath("$.details").value("Process definition cannot be empty")
        );
  }

  @Test
  @SneakyThrows
  void shouldTrowGroupDuplicateProcessDefinitionException() {
    final var version = RandomString.make();
    final var grouping = GroupListDetails.builder().build();
    doThrow(new GroupDuplicateProcessDefinitionException(
        "Has found process definition duplicate")).when(groupingValidator)
        .validate(grouping);

    mockMvc.perform(
            post("/versions/candidates/{versionCandidateId}/business-process-groups", version)
                .content(new ObjectMapper().writeValueAsString(grouping))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnprocessableEntity())
        .andExpectAll(
            jsonPath("$.code").value(GROUPS_PROCESS_DEFINITION_DUPLICATES_EXCEPTION),
            jsonPath("$.details").value("Has found process definition duplicate")
        );
  }

  @Test
  @SneakyThrows
  void shouldTrowGroupNameRegexException() {
    final var version = RandomString.make();
    final var grouping = GroupListDetails.builder().build();
    doThrow(new GroupNameRegexException("Name is not match with regex")).when(groupingValidator)
        .validate(grouping);

    mockMvc.perform(
            post("/versions/candidates/{versionCandidateId}/business-process-groups", version)
                .content(new ObjectMapper().writeValueAsString(grouping))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnprocessableEntity())
        .andExpectAll(
            jsonPath("$.code").value(GROUPS_NAME_REGEX_EXCEPTION),
            jsonPath("$.details").value("Name is not match with regex")
        );

  }

  @Test
  @SneakyThrows
  void shouldTrowGroupNameRequiredException() {
    final var version = RandomString.make();
    final var grouping = GroupListDetails.builder().build();
    doThrow(new GroupNameRequiredException("Group name is mandatory")).when(groupingValidator)
        .validate(grouping);

    mockMvc.perform(
            post("/versions/candidates/{versionCandidateId}/business-process-groups", version)
                .content(new ObjectMapper().writeValueAsString(grouping))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnprocessableEntity())
        .andExpectAll(
            jsonPath("$.code").value(GROUPS_NAME_REQUIRED_EXCEPTION),
            jsonPath("$.details").value("Group name is mandatory")
        );
  }

  @Test
  @SneakyThrows
  void shouldTrowGroupNameUniqueException() {
    final var version = RandomString.make();
    final var grouping = GroupListDetails.builder().build();
    doThrow(new GroupNameUniqueException("Groups name has to be unique")).when(groupingValidator)
        .validate(grouping);

    mockMvc.perform(
            post("/versions/candidates/{versionCandidateId}/business-process-groups", version)
                .content(new ObjectMapper().writeValueAsString(grouping))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnprocessableEntity())
        .andExpectAll(
            jsonPath("$.code").value(GROUPS_NAME_UNIQUE_EXCEPTION),
            jsonPath("$.details").value("Groups name has to be unique")
        );
  }

  @Test
  @SneakyThrows
  void shouldThrowGitFileNotFoundException() {
    var formName = RandomString.make();
    var versionName = RandomString.make();
    doThrow(GitFileNotFoundException.class).when(formService)
        .rollbackForm(formName, versionName);

    mockMvc.perform(
            post("/versions/candidates/{versionCandidateId}/forms/{formName}/rollback", versionName,
                formName))
        .andExpectAll(
            status().isNotFound(),
            response -> Assertions.assertThat(response.getResolvedException())
                .isInstanceOf(GitFileNotFoundException.class),
            jsonPath("$.code").value(is("GIT_FILE_NOT_FOUND_EXCEPTION"))
        );
  }
}