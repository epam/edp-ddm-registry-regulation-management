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

package com.epam.digital.data.platform.management.validation;

import com.epam.digital.data.platform.liquibase.extension.DdmConstants;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Validates table name not to end with '_v' suffix
 */
@RequiredArgsConstructor
@Slf4j
public class TableNameValidator implements ConstraintValidator<TableName, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return !value.endsWith(DdmConstants.SUFFIX_VIEW);
  }
}
