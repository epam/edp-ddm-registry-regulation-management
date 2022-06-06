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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class BuildStatusService {
  private final VersionManagementService versionManagementService;
  private final ControllerMapper mapper;
  public boolean isSuccessCandidateVersionBuild(String versionId) {
    VersionInfoDto versionDetails = versionManagementService.getVersionDetails(versionId);
    Map<String, Integer> labels = versionDetails.getLabels();
    Validation validation = mapper.toValidations(labels).get(0);
    return Objects.equals(validation.getResult(), ResultValues.SUCCESS);
  }

  public boolean isSuccessMasterVersionBuild() {
    var masterInfo = versionManagementService.getMasterInfo();
    String status = Objects.nonNull(masterInfo) ? getStatusVersionBuild(masterInfo, BuildType.MASTER) : "";
    return Objects.equals(status, ResultValues.SUCCESS.name());
  }

  public String getStatusVersionBuild(VersionInfoDto versionInfoDto, BuildType buildType) {
    String status = ResultValues.PENDING.name();
    var messageInfo = versionInfoDto.getMessages().stream()
        .filter(message -> message.message.contains(String.format("MASTER-%s", buildType.getValue())))
        .max(Comparator.comparing(m -> m.date))
        .map(message -> message.message);

    if (messageInfo.isPresent()) {
      String mes = messageInfo.get();
      if (mes.contains("Build Started")){
        status = ResultValues.PENDING.name();
      } else if (mes.contains("Build Successful")) {
        status = ResultValues.SUCCESS.name();
      } else if (mes.contains("Build Failed") || mes.contains("Build Aborted") || mes.contains("Build Unstable")) {
        status = ResultValues.FAILED.name();
      }
    }
    return status;
  }
}
