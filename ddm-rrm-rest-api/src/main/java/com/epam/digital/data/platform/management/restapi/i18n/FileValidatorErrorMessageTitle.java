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

package com.epam.digital.data.platform.management.restapi.i18n;

import com.epam.digital.data.platform.management.restapi.exception.ApplicationExceptionHandler;
import com.epam.digital.data.platform.starter.localization.MessageTitle;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FileValidatorErrorMessageTitle implements MessageTitle {
  ENCODING(ApplicationExceptionHandler.FILE_ENCODING_EXCEPTION,
          "file-validator.error.title.encoding"),
  SIZE(ApplicationExceptionHandler.FILE_SIZE_ERROR,
          "file-validator.error.title.max-size"),
  EXTENSION(ApplicationExceptionHandler.FILE_EXTENSION_ERROR,
          "file-validator.error.title.extension"),
  TOKEN(ApplicationExceptionHandler.JWT_PARSING_ERROR,
          "file-validator.error.title.token"),
  FORM_ALREADY_EXISTS(ApplicationExceptionHandler.FORM_ALREADY_EXISTS_EXCEPTION,
      "file-validator.error.tittle.form-already-exists"),
  TABLE_NOT_FOUND_EXCEPTION(ApplicationExceptionHandler.TABLE_NOT_FOUND_EXCEPTION,
      "file-validator.error.table-not-found"),
  BUSINESS_PROCESS_ALREADY_EXISTS(ApplicationExceptionHandler.BUSINESS_PROCESS_ALREADY_EXISTS_EXCEPTION,
      "file-validator.error.tittle.bp-already-exists"),
  GROUPS_FIELD_REQUIRED_EXCEPTION(ApplicationExceptionHandler.GROUPS_FIELD_REQUIRED_EXCEPTION,
      "group-validator.error.group.required"),
  GROUPS_NAME_REQUIRED_EXCEPTION(ApplicationExceptionHandler.GROUPS_NAME_REQUIRED_EXCEPTION,
      "group-validator.error.group.name.required"),
  GROUPS_NAME_UNIQUE_EXCEPTION(ApplicationExceptionHandler.GROUPS_NAME_UNIQUE_EXCEPTION,
      "group-validator.error.group.name.unique"),
  GROUPS_NAME_REGEX_EXCEPTION(ApplicationExceptionHandler.GROUPS_NAME_REGEX_EXCEPTION,
      "group-validator.error.group.name.regex"),
  GROUPS_PROCESS_DEFINITION_EXCEPTION(ApplicationExceptionHandler.GROUPS_PROCESS_DEFINITION_EXCEPTION,
      "group-validator.error.group.process-definition.required"),
  GROUPS_PROCESS_DEFINITION_DUPLICATES_EXCEPTION(ApplicationExceptionHandler.GROUPS_PROCESS_DEFINITION_DUPLICATES_EXCEPTION,
      "group-validator.error.group.process-definition.duplicate");
  private final String errorCode;
  private final String titleKey;

  public static FileValidatorErrorMessageTitle from(String errorCode) {

    return Stream.of(values())
            .filter(message -> message.errorCode.equals(errorCode))
            .reduce((messageTitle, messageTitle2) -> {
              throw new IllegalStateException("More than 1 message found");
            })
            .orElse(null);
  }
}