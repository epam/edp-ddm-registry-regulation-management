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
import com.epam.digital.data.platform.management.gerritintegration.model.ChangeInfoDto;
import com.google.gerrit.extensions.common.MergeableInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.urswolfer.gerrit.client.rest.http.HttpStatusException;
import java.util.ArrayList;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
public class GerritServiceMrListTest extends AbstractGerritServiceTest {

  @Test
  @SneakyThrows
  void getMrListNotNullTest() {
    var repo = RandomString.make();
    var user = RandomString.make();

    String query = String.format("project:%s+status:open+owner:%s", repo, user);
    Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn(repo);
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn(user);
    Mockito.when(changes.query(query)).thenReturn(request);
    Mockito.when(request.get()).thenReturn(new ArrayList<>());
    Assertions.assertThat(gerritService.getMRList()).isNotNull();
  }

  @Test
  @SneakyThrows
  void getMrListNotEmptyTest() {
    var changeId = RandomString.make();
    changeInfo.changeId = changeId;
    var dto = new ChangeInfoDto();
    dto.setNumber("5");
    var repo = RandomString.make();
    var user = RandomString.make();

    String query = String.format("project:%s+status:open+owner:%s", repo, user);

    Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn(repo);
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn(user);
    Mockito.when(changes.query(query)).thenReturn(request);
    Mockito.when(request.get()).thenReturn(changeInfos);
    Mockito.when(changes.id(changeId)).thenReturn(changeApiRestClient);
    Mockito.when(changeApiRestClient.get()).thenReturn(changeInfo);
    Mockito.when(changeApiRestClient.current()).thenReturn(revisionApi);
    Mockito.when(revisionApi.mergeable()).thenReturn(new MergeableInfo());

    var mrList = gerritService.getMRList();
    Assertions.assertThat(mrList).isNotNull();

    var changeInfo = mrList.get(0);
    Assertions.assertThat( changeInfo.getNumber()).isEqualTo(dto.getNumber());
  }

  @Test
  @SneakyThrows
  void notFoundTest() {
    var repo = RandomString.make();
    var user = RandomString.make();

    String query = String.format("project:%s+status:open+owner:%s", repo, user);

    Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn(repo);
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn(user);
    Mockito.when(changes.query(query)).thenReturn(request);
    Mockito.when(request.get()).thenThrow(
        new HttpStatusException(HttpStatus.NOT_FOUND.value(), "", ""));

    Assertions.assertThatCode(() -> gerritService.getMRList())
        .isInstanceOf(GerritChangeNotFoundException.class);
  }

  @Test
  @SneakyThrows
  void httpExceptionTest() {
    var repo = RandomString.make();
    var user = RandomString.make();

    String query = String.format("project:%s+status:open+owner:%s", repo, user);

    Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn(repo);
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn(user);
    Mockito.when(changes.query(query)).thenReturn(request);
    Mockito.when(request.get()).thenThrow(HttpStatusException.class);

    Assertions.assertThatCode(() -> gerritService.getMRList())
        .isInstanceOf(GerritCommunicationException.class);
  }

  @Test
  @SneakyThrows
  void restApiExceptionTest() {
    var repo = RandomString.make();
    var user = RandomString.make();

    String query = String.format("project:%s+status:open+owner:%s", repo, user);

    Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn(repo);
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn(user);
    Mockito.when(changes.query(query)).thenReturn(request);
    Mockito.when(request.get()).thenThrow(RestApiException.class);

    Assertions.assertThatCode(() -> gerritService.getMRList())
        .isInstanceOf(GerritCommunicationException.class);
  }

}
