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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@DisplayName("TableNameValidator test")
class TableNameValidatorTest {

  @InjectMocks
  private TableNameValidator tableNameValidator;

  @Test
  @DisplayName("should return true if name doesn't end with '_v'")
  void validateName_nameValid() {
    Assertions.assertThat(tableNameValidator.isValid("table_name", null)).isTrue();
  }

  @Test
  @DisplayName("should return false if name ends with '_v'")
  void validateName_nameIsNotValid() {
    Assertions.assertThat(tableNameValidator.isValid("table_name_v", null)).isFalse();
  }

}
