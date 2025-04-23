/*
 * Copyright 2023 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.management.interceptor;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.service.BusinessProcessService;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessProcessETagInterceptor extends AbstractETagHeaderInterceptor {

  private final BusinessProcessService businessProcessService;
  private final GerritPropertiesConfig gerritPropertiesConfig;

  @Override
  protected String getContent(HttpServletRequest request) {
    var paramsMap = getVariables(request);
    var formName = paramsMap.get("businessProcessName");
    var versionCandidateId = paramsMap.get("versionCandidateId");
    if (Objects.isNull(versionCandidateId)) {
      versionCandidateId = gerritPropertiesConfig.getHeadBranch();
    }
    return businessProcessService.getProcessContent(formName, versionCandidateId);
  }
}
