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

import com.epam.digital.data.platform.management.exception.BusinessProcessAlreadyExistsException;
import com.epam.digital.data.platform.management.exception.DataModelFileNotFoundInVersionException;
import com.epam.digital.data.platform.management.exception.ProcessNotFoundException;
import com.epam.digital.data.platform.management.exception.RegistryDataBaseConnectionException;
import com.epam.digital.data.platform.management.exception.TableNotFoundException;
import com.epam.digital.data.platform.management.forms.exception.FormAlreadyExistsException;
import com.epam.digital.data.platform.management.forms.exception.FormNotFoundException;
import com.epam.digital.data.platform.management.gerritintegration.exception.GerritChangeNotFoundException;
import com.epam.digital.data.platform.management.gerritintegration.exception.GerritCommunicationException;
import com.epam.digital.data.platform.management.gerritintegration.exception.GerritConflictException;
import com.epam.digital.data.platform.management.gitintegration.exception.GitCommandException;
import com.epam.digital.data.platform.management.gitintegration.exception.GitFileNotFoundException;
import com.epam.digital.data.platform.management.gitintegration.exception.RepositoryNotFoundException;
import com.epam.digital.data.platform.management.groups.exception.GroupDuplicateProcessDefinitionException;
import com.epam.digital.data.platform.management.groups.exception.GroupEmptyProcessDefinitionException;
import com.epam.digital.data.platform.management.groups.exception.GroupNameRegexException;
import com.epam.digital.data.platform.management.groups.exception.GroupNameRequiredException;
import com.epam.digital.data.platform.management.groups.exception.GroupNameUniqueException;
import com.epam.digital.data.platform.management.groups.exception.GroupsParseException;
import com.epam.digital.data.platform.management.groups.exception.GroupsRequiredException;
import com.epam.digital.data.platform.management.osintegration.exception.GetProcessingException;
import com.epam.digital.data.platform.management.osintegration.exception.OpenShiftInvocationException;
import com.epam.digital.data.platform.management.restapi.i18n.FileValidatorErrorMessageTitle;
import com.epam.digital.data.platform.management.restapi.model.DetailedErrorResponse;
import com.epam.digital.data.platform.management.restapi.validation.ExistingVersionCandidate;
import com.epam.digital.data.platform.management.security.enumeration.Header;
import com.epam.digital.data.platform.management.settings.exception.SettingsParsingException;
import com.epam.digital.data.platform.management.users.exception.CephInvocationException;
import com.epam.digital.data.platform.management.users.exception.FileEncodingException;
import com.epam.digital.data.platform.management.users.exception.FileExtensionException;
import com.epam.digital.data.platform.management.users.exception.FileLoadProcessingException;
import com.epam.digital.data.platform.management.users.exception.JwtParsingException;
import com.epam.digital.data.platform.management.validation.DDMExtensionChangelogFile;
import com.epam.digital.data.platform.management.validation.TableName;
import com.epam.digital.data.platform.management.validation.businessProcess.BusinessProcess;
import com.epam.digital.data.platform.management.versionmanagement.validation.VersionCandidate;
import com.epam.digital.data.platform.starter.localization.MessageResolver;
import java.lang.annotation.Annotation;
import java.util.Objects;
import javax.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ApplicationExceptionHandler extends ResponseEntityExceptionHandler {

  public static final String FILE_ENCODING_EXCEPTION = "FILE_ENCODING_EXCEPTION";
  public static final String FILE_EXTENSION_ERROR = "FILE_EXTENSION_ERROR";
  public static final String FORM_ALREADY_EXISTS_EXCEPTION = "FORM_ALREADY_EXISTS_EXCEPTION";
  public static final String BUSINESS_PROCESS_ALREADY_EXISTS_EXCEPTION = "BUSINESS_PROCESS_ALREADY_EXISTS_EXCEPTION";
  public static final String TABLE_NOT_FOUND_EXCEPTION = "TABLE_NOT_FOUND_EXCEPTION";
  public static final String REGISTRY_DATA_BASE_CONNECTION_ERROR = "REGISTRY_DATA_BASE_CONNECTION_ERROR";
  public static final String FILE_SIZE_ERROR = "FILE_SIZE_ERROR";
  public static final String JWT_PARSING_ERROR = "JWT_PARSING_ERROR";
  public static final String GROUPS_FIELD_REQUIRED_EXCEPTION = "GROUPS_FIELD_REQUIRED_EXCEPTION";
  public static final String GROUPS_NAME_REQUIRED_EXCEPTION = "GROUPS_NAME_REQUIRED_EXCEPTION";
  public static final String GROUPS_NAME_UNIQUE_EXCEPTION = "GROUPS_NAME_UNIQUE_EXCEPTION";
  public static final String GROUPS_NAME_REGEX_EXCEPTION = "GROUPS_NAME_REGEX_EXCEPTION";
  public static final String GROUPS_PROCESS_DEFINITION_EXCEPTION = "GROUPS_PROCESS_DEFINITION_EXCEPTION";
  public static final String GROUPS_PROCESS_DEFINITION_DUPLICATES_EXCEPTION = "GROUPS_PROCESS_DEFINITION_DUPLICATES_EXCEPTION";
  private static final String FORBIDDEN_OPERATION = "FORBIDDEN_OPERATION";
  private static final String IMPORT_CEPH_ERROR = "IMPORT_CEPH_ERROR";
  private static final String GET_CEPH_ERROR = "GET_CEPH_ERROR";
  private static final String RUNTIME_ERROR = "RUNTIME_ERROR";
  private static final String CHANGE_NOT_FOUND = "CHANGE_NOT_FOUND";
  private static final String GERRIT_COMMUNICATION_EXCEPTION = "GERRIT_COMMUNICATION_EXCEPTION";
  private static final String GIT_COMMAND_ERROR = "GIT_COMMAND_ERROR";
  public static final String CONFLICT_ERROR = "CONFLICT_ERROR";
  private static final String SETTINGS_PARSING_EXCEPTION = "SETTINGS_PARSING_EXCEPTION";
  private static final String GROUPS_PARSING_EXCEPTION = "GROUPS_PARSING_EXCEPTION";
  private static final String BUSINESS_PROCESS_CONTENT_EXCEPTION = "BUSINESS_PROCESS_CONTENT_EXCEPTION";
  private static final String INVALID_VERSION_CANDIDATE_EXCEPTION = "INVALID_VERSION_CANDIDATE_EXCEPTION";
  private static final String CONSTRAINT_VIOLATION_EXCEPTION = "CONSTRAINT_VIOLATION_EXCEPTION";
  private static final String REPOSITORY_NOT_FOUND_EXCEPTION = "REPOSITORY_NOT_FOUND_EXCEPTION";
  private static final String FORM_NOT_FOUND_EXCEPTION = "FORM_NOT_FOUND_EXCEPTION";
  private static final String PROCESS_NOT_FOUND_EXCEPTION = "PROCESS_NOT_FOUND_EXCEPTION";
  private static final String GIT_FILE_NOT_FOUND_EXCEPTION = "GIT_FILE_NOT_FOUND_EXCEPTION";
  private static final String ETAG_FILTERING_EXCEPTION = "ETAG_FILTERING_EXCEPTION";
  private static final String DATA_MODEL_FILE_NOT_FOUND = "DATA_MODEL_FILE_NOT_FOUND";
  private static final String INVALID_CHANGELOG = "INVALID_CHANGELOG";

  private final MessageResolver messageResolver;

  @ExceptionHandler(Exception.class)
  public ResponseEntity<DetailedErrorResponse> handleException(Exception exception) {
    log.error("Runtime error occurred", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(RUNTIME_ERROR, exception));
  }

  @ExceptionHandler(FileLoadProcessingException.class)
  public ResponseEntity<DetailedErrorResponse> handleFileLoadProcessingException(
      FileLoadProcessingException exception) {
    log.error("Error during upload to Ceph", exception);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(newDetailedResponse(IMPORT_CEPH_ERROR, exception));
  }

  @ExceptionHandler(GitCommandException.class)
  public ResponseEntity<DetailedErrorResponse> handle(GitCommandException exception) {
    log.error("Could not perform git command", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(GIT_COMMAND_ERROR, exception));
  }

  @ExceptionHandler(GetProcessingException.class)
  public ResponseEntity<DetailedErrorResponse> handleGetProcessingException(
      GetProcessingException exception) {
    log.error("Error during getting from Ceph", exception);
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(newDetailedResponse(GET_CEPH_ERROR, exception));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<DetailedErrorResponse> handleAccessDeniedException(
      AccessDeniedException exception) {
    log.error("Access denied", exception);
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(newDetailedResponse(FORBIDDEN_OPERATION, exception));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<DetailedErrorResponse> handleMaxUploadSizeExceededException(
      MaxUploadSizeExceededException exception) {
    log.error("File is to large", exception);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(newDetailedResponse(FILE_SIZE_ERROR, exception));
  }

  @ExceptionHandler(CephInvocationException.class)
  public ResponseEntity<DetailedErrorResponse> handleCephInvocationException(
      CephInvocationException exception) {
    log.error("Ceph invocation exception", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(RUNTIME_ERROR, exception));
  }

  @ExceptionHandler(OpenShiftInvocationException.class)
  public ResponseEntity<DetailedErrorResponse> handleOpenShiftInvocationException(
      OpenShiftInvocationException exception) {
    log.error("Open-shift invocation exception", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(RUNTIME_ERROR, exception));
  }

  @ExceptionHandler(JwtParsingException.class)
  public ResponseEntity<DetailedErrorResponse> handleJwtParsingException(
      JwtParsingException exception) {
    log.error("Jwt parsing exception", exception);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(newDetailedResponse(JWT_PARSING_ERROR, exception));
  }

  @ExceptionHandler(FileEncodingException.class)
  public ResponseEntity<DetailedErrorResponse> handleFileEncodingException(
      FileEncodingException exception) {
    log.error("File encoding exception", exception);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(newDetailedResponse(FILE_ENCODING_EXCEPTION, exception));
  }

  @ExceptionHandler(FileExtensionException.class)
  public ResponseEntity<DetailedErrorResponse> handleFileExtensionException(
      FileExtensionException exception) {
    log.error("File extension exception", exception);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(newDetailedResponse(FILE_EXTENSION_ERROR, exception));
  }

  @ExceptionHandler(FormAlreadyExistsException.class)
  public ResponseEntity<DetailedErrorResponse> handleFormAlreadyExistsException(
      FormAlreadyExistsException exception) {
    log.error("Form already exists exception", exception);
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(newDetailedResponse(FORM_ALREADY_EXISTS_EXCEPTION, exception));
  }

  @ExceptionHandler(TableNotFoundException.class)
  public ResponseEntity<DetailedErrorResponse> handleTableNotFoundException(
      TableNotFoundException exception) {
    log.warn("Table not found exception", exception);
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(newDetailedResponse(TABLE_NOT_FOUND_EXCEPTION, exception));
  }

  @ExceptionHandler(RegistryDataBaseConnectionException.class)
  public ResponseEntity<DetailedErrorResponse> handleTableParseException(
      RegistryDataBaseConnectionException exception) {
    log.error("Couldn't connect to master version database", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(REGISTRY_DATA_BASE_CONNECTION_ERROR, exception));
  }

  @ExceptionHandler
  public ResponseEntity<DetailedErrorResponse> handleGerritChangeNotFoundException(
      GerritChangeNotFoundException exception) {
    log.error("Change not found", exception);
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(newDetailedResponse(CHANGE_NOT_FOUND, exception));
  }

  @ExceptionHandler
  public ResponseEntity<DetailedErrorResponse> handleGerritCommunicationException(
      GerritCommunicationException exception) {
    log.error("Something went wrong with accessing gerrit", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(GERRIT_COMMUNICATION_EXCEPTION, exception));
  }

  @ExceptionHandler
  public ResponseEntity<DetailedErrorResponse> handleGerritConflictException(
      GerritConflictException exception) {
    log.error("Conflict occurred on version candidate submission", exception);
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(newDetailedResponse(CONFLICT_ERROR, exception));
  }

  @ExceptionHandler(BusinessProcessAlreadyExistsException.class)
  public ResponseEntity<DetailedErrorResponse> handleBusinessProcessAlreadyExistsException(
      BusinessProcessAlreadyExistsException exception) {
    log.error("Business process already exists exception", exception);
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(newDetailedResponse(BUSINESS_PROCESS_ALREADY_EXISTS_EXCEPTION, exception));
  }

  @ExceptionHandler
  public ResponseEntity<DetailedErrorResponse> handleWritingRepositoryException(
      SettingsParsingException exception) {
    log.error("Could not parse settings files");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(SETTINGS_PARSING_EXCEPTION, exception));
  }

  @ExceptionHandler
  public ResponseEntity<DetailedErrorResponse> handleFormNotFoundException(
      FormNotFoundException exception) {
    log.error("Form {} not found", exception.getFormName());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(newDetailedResponse(FORM_NOT_FOUND_EXCEPTION, exception));
  }

  @ExceptionHandler
  public ResponseEntity<DetailedErrorResponse> handleProcessNotFoundException(
      ProcessNotFoundException exception) {
    log.error("Business process {} not found", exception.getProcessName());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(newDetailedResponse(PROCESS_NOT_FOUND_EXCEPTION, exception));
  }

  @ExceptionHandler
  public ResponseEntity<DetailedErrorResponse> handleRepositoryNotFoundException(
      RepositoryNotFoundException exception) {
    log.error("Repository {} not found", exception.getVersionId());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(newDetailedResponse(REPOSITORY_NOT_FOUND_EXCEPTION, exception));
  }

  @ExceptionHandler
  public ResponseEntity<DetailedErrorResponse> handleGitFileNotFoundException(
      GitFileNotFoundException exception) {
    log.error("File {} not found", exception.getFileName());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(newDetailedResponse(GIT_FILE_NOT_FOUND_EXCEPTION, exception));
  }

  @ExceptionHandler
  public ResponseEntity<DetailedErrorResponse> handleConstraintViolationException(
      ConstraintViolationException exception) {
    var annotation = getAnnotationFromConstraintViolationException(exception);
    if (annotation instanceof BusinessProcess) {
      log.warn("Business process content has errors");
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
          .body(newDetailedResponse(BUSINESS_PROCESS_CONTENT_EXCEPTION, exception));
    }
    if (annotation instanceof VersionCandidate) {
      log.warn("Version candidate name or description is invalid");
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
          .body(newDetailedResponse(INVALID_VERSION_CANDIDATE_EXCEPTION, exception));
    }
    if (annotation instanceof ExistingVersionCandidate) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(newDetailedResponse(CHANGE_NOT_FOUND, exception));
    }
    if (annotation instanceof TableName) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(newDetailedResponse(HttpStatus.BAD_REQUEST.getReasonPhrase(), exception));
    }
    if (annotation instanceof DDMExtensionChangelogFile) {
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
          .body(newDetailedResponse(INVALID_CHANGELOG, exception));
    }
    log.error("Constraint violation exception");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(CONSTRAINT_VIOLATION_EXCEPTION, exception));
  }

  @ExceptionHandler
  public ResponseEntity<DetailedErrorResponse> handleETagFilteringException(
      ETagFilteringException exception) {
    log.error("Invalid ETag for {} form from {} version candidate", exception.getFormName(),
        exception.getVersionCandidate());
    return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
        .body(newDetailedResponse(ETAG_FILTERING_EXCEPTION, exception));
  }

  @ExceptionHandler
  public ResponseEntity<DetailedErrorResponse> handleDataModelFileNotFoundInVersionException(
      DataModelFileNotFoundInVersionException exception) {
    log.warn("Data-model file '{}' wasn't found in version '{}'", exception.getFilePath(),
        exception.getVersionId());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(newDetailedResponse(DATA_MODEL_FILE_NOT_FOUND, exception));
  }

  @ExceptionHandler(GroupsParseException.class)
  public ResponseEntity<DetailedErrorResponse> handleParseGroupsException(
      GroupsParseException exception) {
    log.error("Could not parse groups file");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(newDetailedResponse(GROUPS_PARSING_EXCEPTION, exception));
  }
  @ExceptionHandler(GroupEmptyProcessDefinitionException.class)
  public ResponseEntity<DetailedErrorResponse> handleGroupEmptyProcessDefinitionException(
      GroupEmptyProcessDefinitionException exception) {
    log.error("Process definitions cannot be empty");
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(newDetailedResponse(GROUPS_PROCESS_DEFINITION_EXCEPTION, exception));
  }

  @ExceptionHandler(GroupNameRegexException.class)
  public ResponseEntity<DetailedErrorResponse> handleGroupNameRegexException(
      GroupNameRegexException exception) {
    log.error("Group name doesn't match with regex");
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(newDetailedResponse(GROUPS_NAME_REGEX_EXCEPTION, exception));
  }

  @ExceptionHandler(GroupNameRequiredException.class)
  public ResponseEntity<DetailedErrorResponse> handleGroupNameRequiredException(
      GroupNameRequiredException exception) {
    log.error("Group name is mandatory field");
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(newDetailedResponse(GROUPS_NAME_REQUIRED_EXCEPTION, exception));
  }

  @ExceptionHandler(GroupNameUniqueException.class)
  public ResponseEntity<DetailedErrorResponse> handleGroupNameUniqueException(
      GroupNameUniqueException exception) {
    log.error("Group names has to be unique");
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(newDetailedResponse(GROUPS_NAME_UNIQUE_EXCEPTION, exception));
  }

  @ExceptionHandler(GroupsRequiredException.class)
  public ResponseEntity<DetailedErrorResponse> handleGroupsRequiredException(
      GroupsRequiredException exception) {
    log.error("Groups are mandatory field");
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(newDetailedResponse(GROUPS_FIELD_REQUIRED_EXCEPTION, exception));
  }
  @ExceptionHandler(GroupDuplicateProcessDefinitionException.class)
  public ResponseEntity<DetailedErrorResponse> handleGroupDuplicateProcessDefinitionException(
      GroupDuplicateProcessDefinitionException exception) {
    log.error("Has found process definition duplicates");
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(newDetailedResponse(GROUPS_PROCESS_DEFINITION_DUPLICATES_EXCEPTION, exception));
  }


  private Annotation getAnnotationFromConstraintViolationException(
      ConstraintViolationException exception) {
    var constraintViolations = exception.getConstraintViolations();
    if (constraintViolations != null && !constraintViolations.isEmpty()) {
      var next = constraintViolations.iterator().next();
      if (next != null && next.getConstraintDescriptor() != null) {
        return next.getConstraintDescriptor().getAnnotation();
      }
    }
    return null;
  }

  private DetailedErrorResponse newDetailedResponse(String code, Exception exception) {
    var response = new DetailedErrorResponse();
    response.setTraceId(MDC.get(Header.TRACE_ID.getHeaderName()));
    response.setCode(code);
    response.setDetails(exception.getMessage());

    var titleKey = FileValidatorErrorMessageTitle.from(code);
    response.setLocalizedMessage(
        Objects.isNull(titleKey) ? null : messageResolver.getMessage(titleKey));

    return response;
  }
}
