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

import static com.epam.digital.data.platform.management.util.InitialisationUtils.initChangeInfo;
import static com.epam.digital.data.platform.management.util.InitialisationUtils.initChangeInfoDto;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.RevisionInfo;
import java.util.HashMap;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

public class CandidateVersionSettingsControllerIT extends BaseIT {

  private static final String BASE_REQUEST = "/versions/candidates/";

  private static final String GLOBAL_SETTINGS_VALUE =
      "supportEmail: \"support@registry.gov.ua\"\n" +
          "themeFile: \"white-theme.js\"\n";
  private static final String SETTINGS_VALUE = "settings:\n" +
      "  general:\n" +
      "    validation:\n" +
      "      email:\n" +
      "        blacklist:\n" +
      "          domains:\n" +
      "          - \"ya.ua\"\n" +
      "          - \"ya.ru\"\n" +
      "    titleFull: \"<Registry name>\"\n" +
      "    title: \"mdtuddm\"\n";

  @Test
  @SneakyThrows
  void getSettings() {
    String versionCandidateId = "1";
    String formName = "formName";

    ChangeInfo changeInfo = initChangeInfo(1, "admin", "admin@epam.com", "admin");
    ChangeInfoDto changeInfoDto = initChangeInfoDto(versionCandidateId);
    changeInfo.revisions = new HashMap<>();
    RevisionInfo revisionInfo = new RevisionInfo();
    revisionInfo.ref = versionCandidateId;
    changeInfo.revisions.put(formName, revisionInfo);
    changeInfo.currentRevision = formName;
    changeInfoDto.setRefs(versionCandidateId);
    final var versionCandidateCloneResult = jGitWrapperMock.mockCloneCommand(versionCandidateId);
    jGitWrapperMock.mockGetSettings(SETTINGS_VALUE, GLOBAL_SETTINGS_VALUE);
    jGitWrapperMock.mockCheckoutCommand();
    jGitWrapperMock.mockFetchCommand(changeInfoDto);
    jGitWrapperMock.mockPullCommand();
    gerritApiMock.mockGetChangeInfo(versionCandidateId, changeInfo);
    mockMvc.perform(MockMvcRequestBuilders.get(BASE_REQUEST + versionCandidateId + "/settings")
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpectAll(
            status().isOk(),
            content().contentType("application/json"),
            jsonPath("$.supportEmail", is("support@registry.gov.ua")),
            jsonPath("$.themeFile", is("white-theme.js")),
            jsonPath("$.titleFull", is("<Registry name>")),
            jsonPath("$.blacklistedDomains", hasSize(2)),
            jsonPath("title", is("mdtuddm")));

    Mockito.verify(versionCandidateCloneResult).close();
  }
}
