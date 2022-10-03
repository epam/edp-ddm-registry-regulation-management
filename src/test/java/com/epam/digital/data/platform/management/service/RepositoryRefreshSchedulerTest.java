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

package com.epam.digital.data.platform.management.service;

import static org.mockito.Mockito.never;

import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.epam.digital.data.platform.management.service.impl.GerritServiceImpl;
import com.epam.digital.data.platform.management.service.impl.JGitServiceImpl;
import com.epam.digital.data.platform.management.service.impl.RepositoryRefreshScheduler;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.LabelInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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
    ChangeInfo changeInfo = getChangeInfo();
    Mockito.when(gerritService.getMRList()).thenReturn(List.of(changeInfo));
  }

  @Test
  @SneakyThrows
  void refreshTest() {
    ChangeInfo changeInfo = getChangeInfo();
    ChangeInfoDto changeInfoDto = getChangeInfoDto();

    Mockito.when(gerritService.getChangeInfo(changeInfo.changeId)).thenReturn(changeInfoDto);

    repositoryRefreshScheduler.refresh();
    Mockito.verify(gerritService).rebase(changeInfo.changeId);
    Mockito.verify(jGitService).fetch(changeInfoDto.getNumber(), changeInfoDto);
    Mockito.verify(jGitService).resetHeadBranchToRemote();
  }

  @Test
  @SneakyThrows
  void refreshRebaseErrorTest() {
    ChangeInfo changeInfo = getChangeInfo();
    ChangeInfoDto changeInfoDto = getChangeInfoDto();

    Mockito.doThrow(RestApiException.class).when(gerritService).rebase(changeInfo.changeId);

    Assertions.assertThatCode(() -> repositoryRefreshScheduler.refresh())
        .doesNotThrowAnyException();
    Mockito.verify(jGitService, never()).fetch(changeInfoDto.getNumber(), changeInfoDto);
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

  private ChangeInfoDto getChangeInfoDto() {
    ChangeInfoDto changeInfoDto = new ChangeInfoDto();
    changeInfoDto.setSubject("change");
    changeInfoDto.setNumber("1");
    changeInfoDto.setRefs("refs");
    return changeInfoDto;
  }

  private ChangeInfo getChangeInfo() {
    var changeInfo = new ChangeInfo();
    changeInfo.id = "changeInfoId";
    changeInfo._number = 1;
    changeInfo.changeId = "changeInfoChangeId";
    changeInfo.branch = "changeInfoBranch";
    changeInfo.created = Timestamp.from(
        LocalDateTime.of(2022, 8, 10, 17, 15).toInstant(ZoneOffset.ofHours(3)));
    changeInfo.subject = "changeInfoSubject";
    changeInfo.topic = "changeInfoTopic";
    changeInfo.project = "changeInfoProject";
    changeInfo.submitted = Timestamp.from(
        LocalDateTime.of(2022, 8, 10, 17, 25).toInstant(ZoneOffset.ofHours(3)));
    changeInfo.updated = Timestamp.from(
        LocalDateTime.of(2022, 8, 10, 17, 35).toInstant(ZoneOffset.ofHours(3)));
    changeInfo.owner = new AccountInfo(1);
    changeInfo.owner.username = "changeInfoOwnerUsername";
    changeInfo.mergeable = false;
    changeInfo.labels = Map.of("label1", new LabelInfo(), "label2", new LabelInfo());
    changeInfo.labels.get("label1").approved = new AccountInfo(2);
    return changeInfo;
  }

}
