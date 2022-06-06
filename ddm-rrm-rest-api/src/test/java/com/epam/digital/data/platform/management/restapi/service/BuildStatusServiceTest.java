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

package com.epam.digital.data.platform.management.restapi.service;

import com.epam.digital.data.platform.management.restapi.mapper.ControllerMapper;
import com.epam.digital.data.platform.management.restapi.model.BuildType;
import com.epam.digital.data.platform.management.restapi.model.ResultValues;
import com.epam.digital.data.platform.management.restapi.model.Validation;
import com.epam.digital.data.platform.management.versionmanagement.model.VersionInfoDto;
import com.epam.digital.data.platform.management.versionmanagement.service.VersionManagementService;
import com.google.gerrit.extensions.common.ChangeMessageInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class BuildStatusServiceTest {
  public static final String BUILD_STARTED_MASTER_BUILD = "Build Started ... MASTER-Build ...";
  public static final String BUILD_SUCCESSFUL_MASTER_BUILD = "Build Successful ... MASTER-Build ...";
  public static final String BUILD_FAILED_MASTER_BUILD = "Build Failed ... MASTER-Build ...";
  public static final String BUILD_ABORTED_MASTER_BUILD = "Build Aborted ... MASTER-Build ...";
  public static final String BUILD_SUCCESSFUL_MASTER_CODE_REVIEW = "Build Successful ... MASTER-Code-review ...";
  @Mock
  VersionManagementService versionManagementService;
  @Mock
  ControllerMapper mapper;

  @InjectMocks
  BuildStatusService buildStatusService;

  @ParameterizedTest
  @EnumSource(ResultValues.class)
  void isSuccessCandidateVersionBuild(ResultValues status) {
    VersionInfoDto versionInfoDto = mock(VersionInfoDto.class);
    when(versionManagementService.getVersionDetails("someId")).thenReturn(versionInfoDto);
    Map<String, Integer> labels =  Map.of("Verified", 1);
    when(versionInfoDto.getLabels()).thenReturn(labels);
    Validation validation = mock(Validation.class);
    when(mapper.toValidations(labels)).thenReturn(List.of(validation));
    when(validation.getResult()).thenReturn(status);

    boolean isSuccess = buildStatusService.isSuccessCandidateVersionBuild("someId");

    if (status.equals(ResultValues.SUCCESS)){
      assertTrue(isSuccess);
    } else {
      assertFalse(isSuccess);
    }
  }

  @ParameterizedTest
  @MethodSource("provideBuildStatuses")
  void isSuccessMasterVersionBuild(String message) {
    VersionInfoDto versionInfoDto = mock(VersionInfoDto.class);
    when(versionManagementService.getMasterInfo()).thenReturn(versionInfoDto);
    ChangeMessageInfo changeMessageInfo = new ChangeMessageInfo();
    changeMessageInfo.message = message;
    changeMessageInfo.date = Timestamp.valueOf(LocalDateTime.now());
    when(versionInfoDto.getMessages()).thenReturn(List.of(changeMessageInfo));

    boolean isSuccess = buildStatusService.isSuccessMasterVersionBuild();

    if (message.equals(BUILD_SUCCESSFUL_MASTER_BUILD)) {
      assertTrue(isSuccess);
    } else {
      assertFalse(isSuccess);
    }
  }

  @Test
  void isSuccessMasterVersionBuild_shouldReturnFalseWhenMasterInfoIsNull() {
    when(versionManagementService.getMasterInfo()).thenReturn(null);
    assertFalse(buildStatusService.isSuccessMasterVersionBuild());
  }

  @ParameterizedTest
  @MethodSource("provideBuildStatuses")
  void getStatusMasterVersionBuild(String message, String expectedStatus) {
    VersionInfoDto versionInfoDto = mock(VersionInfoDto.class);
    when(versionManagementService.getMasterInfo()).thenReturn(versionInfoDto);
    ChangeMessageInfo changeMessageInfo = new ChangeMessageInfo();
    changeMessageInfo.message = message;
    changeMessageInfo.date = Timestamp.valueOf(LocalDateTime.now());
    when(versionInfoDto.getMessages()).thenReturn(List.of(changeMessageInfo));

    var status = buildStatusService.getStatusVersionBuild(versionInfoDto, BuildType.MASTER);

    assertEquals(expectedStatus, status);
  }

  static Stream<Arguments> provideBuildStatuses() {
    return Stream.of(
        arguments(BUILD_STARTED_MASTER_BUILD, ResultValues.PENDING.name()),
        arguments(BUILD_SUCCESSFUL_MASTER_BUILD, ResultValues.SUCCESS.name()),
        arguments(BUILD_FAILED_MASTER_BUILD, ResultValues.FAILED.name()),
        arguments(BUILD_ABORTED_MASTER_BUILD, ResultValues.FAILED.name()),
        arguments(BUILD_SUCCESSFUL_MASTER_CODE_REVIEW, ResultValues.PENDING.name())
    );
  }
}