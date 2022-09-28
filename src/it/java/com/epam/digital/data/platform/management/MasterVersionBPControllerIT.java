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

package com.epam.digital.data.platform.management;

import com.epam.digital.data.platform.management.model.dto.BusinessProcessDetailsShort;
import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.RevisionInfo;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.HashMap;
import java.util.Map;

import static com.epam.digital.data.platform.management.util.InitialisationUtils.*;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class MasterVersionBPControllerIT extends BaseIT {

  private static final String BASE_REQUEST = "/versions/master/business-processes";

  @Test
  @SneakyThrows
  public void getBusinessProcess() {
    String versionCandidateId = "head-branch";
    String businessProcessName = "name";
    ChangeInfo changeInfo = initChangeInfo(1, "admin", "admin@epam.com", "admin");
    ChangeInfoDto changeInfoDto = initChangeInfoDto(versionCandidateId);
    BusinessProcessDetailsShort businessProcessDetails = initBusinessProcessDetails(businessProcessName, "title");

    changeInfo.revisions = new HashMap<>();
    RevisionInfo revisionInfo = new RevisionInfo();
    revisionInfo.ref = versionCandidateId;
    changeInfo.revisions.put(businessProcessName, revisionInfo);
    changeInfo.currentRevision = businessProcessName;
    changeInfoDto.setRefs(versionCandidateId);

    jGitWrapperMock.mockCloneCommand(versionCandidateId);
    jGitWrapperMock.mockGetBusinessProcess(businessProcess);
    jGitWrapperMock.mockCheckoutCommand();
    jGitWrapperMock.mockPullCommand();
    jGitWrapperMock.mockFetchCommand(changeInfoDto);
    gerritApiMock.mockGetMRByNumber(versionCandidateId, changeInfo);

    mockMvc.perform(MockMvcRequestBuilders.get(BASE_REQUEST + "/{businessProcessName}", versionCandidateId, businessProcessName)
        .accept(MediaType.TEXT_XML)).andExpectAll(
        status().isOk(),
        content().contentType("text/xml"),
        xpath("/bpmn:definitions/bpmn:process/@id", BPMN_NAMESPACES).string(businessProcessDetails.getName()),
        xpath("/bpmn:definitions/bpmn:process/@name", BPMN_NAMESPACES).string(businessProcessDetails.getTitle())
    );
  }

  @Test
  @SneakyThrows
  public void getBusinessProcesses() {
    String versionCandidateId = "head-branch";
    String businessProcessName = "businessProcessName";
    ChangeInfo changeInfo = initChangeInfo(1, "admin", "admin@epam.com", "admin");
    ChangeInfoDto changeInfoDto = initChangeInfoDto(versionCandidateId);

    changeInfo.revisions = new HashMap<>();
    RevisionInfo revisionInfo = new RevisionInfo();
    revisionInfo.ref = versionCandidateId;
    changeInfo.revisions.put(businessProcessName, revisionInfo);
    changeInfo.currentRevision = businessProcessName;
    changeInfoDto.setRefs(versionCandidateId);

    jGitWrapperMock.mockCloneCommand(versionCandidateId);
    jGitWrapperMock.mockGetBusinessProcessList(Map.of("name", businessProcess));
    jGitWrapperMock.mockCheckoutCommand();
    jGitWrapperMock.mockPullCommand();
    jGitWrapperMock.mockLogCommand();
    jGitWrapperMock.mockFetchCommand(changeInfoDto);
    jGitWrapperMock.mockCheckoutCommand();
    gerritApiMock.mockGetMRByNumber(versionCandidateId, changeInfo);
    mockMvc.perform(MockMvcRequestBuilders.get(BASE_REQUEST, versionCandidateId)
        .accept(MediaType.APPLICATION_JSON)).andExpectAll(
        status().isOk(),
        content().contentType(MediaType.APPLICATION_JSON),
        jsonPath("$", hasSize(1)));
  }
}
