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

package com.epam.digital.data.platform.management.scheduled;

import static org.mockito.Mockito.never;

import com.epam.digital.data.platform.management.gerritintegration.exception.GerritCommunicationException;
import com.epam.digital.data.platform.management.gerritintegration.model.ChangeInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.service.GerritServiceImpl;
import com.epam.digital.data.platform.management.gitintegration.service.JGitServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class RepositoryRefreshSchedulerTest {

  @Mock
  private JGitServiceImpl jGitService;
  @Mock
  private GerritServiceImpl gerritService;

  @InjectMocks
  private RepositoryRefreshScheduler repositoryRefreshScheduler;

  @BeforeEach
  @SneakyThrows
  void mockMethods() {
    ChangeInfoDto changeInfo = getChangeInfo();
    Mockito.when(gerritService.getMRList()).thenReturn(List.of(changeInfo));
  }

  @Test
  @SneakyThrows
  void refreshTest() {
    ChangeInfoDto changeInfoDto = getChangeInfo();

    Mockito.when(gerritService.getChangeInfo(changeInfoDto.getChangeId())).thenReturn(changeInfoDto);

    repositoryRefreshScheduler.refresh();
    Mockito.verify(gerritService).rebase(changeInfoDto.getChangeId());
    Mockito.verify(jGitService).fetch(changeInfoDto.getNumber(), changeInfoDto.getRefs());
    Mockito.verify(jGitService).resetHeadBranchToRemote();
  }

  @Test
  @SneakyThrows
  void refreshRebaseErrorTest() {
    ChangeInfoDto changeInfoDto = getChangeInfo();

    Mockito.doThrow(GerritCommunicationException.class).when(gerritService).rebase(changeInfoDto.getChangeId());

    Assertions.assertThatCode(() -> repositoryRefreshScheduler.refresh())
        .doesNotThrowAnyException();
    Mockito.verify(jGitService, never()).fetch(changeInfoDto.getNumber(), changeInfoDto.getRefs());
    Mockito.verify(jGitService).resetHeadBranchToRemote();
  }

  @Test
  @SneakyThrows
  void refreshHeadBranchRefreshErrorTest() {
    Mockito.doThrow(RuntimeException.class).when(jGitService).resetHeadBranchToRemote();

    Assertions.assertThatCode(() -> repositoryRefreshScheduler.refresh())
        .doesNotThrowAnyException();

    Mockito.verify(jGitService).resetHeadBranchToRemote();
  }


  private ChangeInfoDto getChangeInfo() {
    var changeInfo = new ChangeInfoDto();
    changeInfo.setId("changeInfoId");
    changeInfo.setNumber("1");
    changeInfo.setChangeId("changeInfoChangeId");
    changeInfo.setBranch("changeInfoBranch");
    changeInfo.setCreated(
        LocalDateTime.of(2022, 8, 10, 17, 15));
    changeInfo.setSubject("changeInfoSubject");
    changeInfo.setTopic("changeInfoTopic");
    changeInfo.setProject("changeInfoProject");
    changeInfo.setSubmitted(
        LocalDateTime.of(2022, 8, 10, 17, 25));
    changeInfo.setUpdated(
        LocalDateTime.of(2022, 8, 10, 17, 35));
    changeInfo.setOwner("changeInfoOwnerUsername");
    changeInfo.setMergeable(false);
    changeInfo.setLabels(Map.of("label1", 1, "label2", -1));
    return changeInfo;
  }

}
