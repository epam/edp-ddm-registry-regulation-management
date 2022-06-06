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

package com.epam.digital.data.platform.management.restapi.validation;

import com.epam.digital.data.platform.management.gerritintegration.exception.GerritChangeNotFoundException;
import com.epam.digital.data.platform.management.versionmanagement.service.VersionManagementService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@DisplayName("ExistingVersionCandidateValidator#isValid")
class ExistingVersionCandidateValidatorTest {

  @Mock
  VersionManagementService versionManagementService;
  @InjectMocks
  ExistingVersionCandidateValidator validator;

  @Test
  @DisplayName("should return true if versionManagementService didn't throw a GerritChangeNotFoundException")
  void versionCandidateExists() {
    var versionId = 192;

    Mockito.doReturn(null).when(versionManagementService).getVersionDetails("192");

    var result = validator.isValid(versionId, null);

    Assertions.assertThat(result).isTrue();
  }

  @Test
  @DisplayName("should return false if versionManagementService threw a GerritChangeNotFoundException")
  void versionCandidateDoesNotExists() {
    var versionId = 192;

    Mockito.doThrow(GerritChangeNotFoundException.class)
        .when(versionManagementService).getVersionDetails("192");

    var result = validator.isValid(versionId, null);

    Assertions.assertThat(result).isFalse();
  }

  @Test
  @DisplayName("should rethrow if versionManagementService threw any exception except GerritChangeNotFoundException")
  void versionManagementServiceThrewUnexpectedException() {
    var versionId = 192;
    var ex = new RuntimeException();
    Mockito.doThrow(ex).when(versionManagementService).getVersionDetails("192");

    Assertions.assertThatThrownBy(() -> validator.isValid(versionId, null))
        .isSameAs(ex);
  }
}
