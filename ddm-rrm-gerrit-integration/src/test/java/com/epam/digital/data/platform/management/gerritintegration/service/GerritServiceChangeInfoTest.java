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
package com.epam.digital.data.platform.management.gerritintegration.service;


import com.epam.digital.data.platform.management.gerritintegration.exception.GerritChangeNotFoundException;
import com.epam.digital.data.platform.management.gerritintegration.exception.GerritCommunicationException;
import com.google.gerrit.extensions.common.RevisionInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.urswolfer.gerrit.client.rest.http.HttpStatusException;
import java.util.HashMap;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
public class GerritServiceChangeInfoTest extends AbstractGerritServiceTest {

  @Test
  @SneakyThrows
  void getChangeInfoTest() {
    var refs = RandomString.make();
    var changeId = RandomString.make();

    var revisionInfo = new RevisionInfo();
    revisionInfo.ref = refs;
    HashMap<String, RevisionInfo> revisionsMap = new HashMap<>();
    revisionsMap.put(null, revisionInfo);
    changeInfo.revisions = revisionsMap;
    changeInfo.changeId = changeId;

    Mockito.when(changes.id(changeId)).thenReturn(changeApiRestClient);
    Mockito.when(changeApiRestClient.get()).thenReturn(changeInfo);

    var changeInfoDto = gerritService.getChangeInfo(changeId);
    Assertions.assertThat(changeInfoDto.getChangeId()).isEqualTo(changeId);
    Assertions.assertThat(changeInfoDto.getRefs()).isEqualTo(refs);
    Assertions.assertThat(changeInfoDto.getNumber()).isEqualTo("5");
  }

  @Test
  @SneakyThrows
  void notFoundExceptionTest() {
    var changeId = RandomString.make();
    Mockito.when(changes.id(changeId)).thenThrow(
        new HttpStatusException(HttpStatus.NOT_FOUND.value(), "", ""));

    Assertions.assertThatCode(() -> gerritService.getChangeInfo(changeId))
        .isInstanceOf(GerritChangeNotFoundException.class);
  }

  @Test
  @SneakyThrows
  void httpStatusExceptionTest() {
    var changeId = RandomString.make();
    Mockito.when(changes.id(changeId)).thenThrow(HttpStatusException.class);

    Assertions.assertThatCode(() -> gerritService.getChangeInfo(changeId))
        .isInstanceOf(GerritCommunicationException.class);
  }

  @Test
  @SneakyThrows
  void restApiExceptionTest() {
    var changeId = RandomString.make();
    Mockito.when(changes.id(changeId)).thenThrow(RestApiException.class);

    Assertions.assertThatCode(() -> gerritService.getChangeInfo(changeId))
        .isInstanceOf(GerritCommunicationException.class);
  }

}
