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
import com.google.gerrit.extensions.api.changes.ChangeApi;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gson.JsonElement;
import com.urswolfer.gerrit.client.rest.http.HttpStatusException;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
public class GerritServiceGetFileContentTest extends AbstractGerritServiceTest{

  @Test
  @SneakyThrows
  void getFileContentTest() {
    var fileContent = gerritService.getFileContent(null, "");

    Assertions.assertThat(fileContent).isNull();
  }

  @Test
  @SneakyThrows
  void getFileContentSuccessTest() {
    var changeId = RandomString.make();
    var filePath = RandomString.make();
    changeInfo.currentRevision = RandomString.make();

    var changeApi = Mockito.mock(ChangeApi.class);

    Mockito.when(gerritApiImpl.changes()).thenReturn(changes);
    Mockito.when(changes.id(changeId)).thenReturn(changeApi);
    Mockito.when(changeApi.get()).thenReturn(changeInfo);
    Mockito.when(gerritApiImpl.restClient()).thenReturn(gerritRestClient);

    String request = String.format("/changes/%s/revisions/%s/files/%s/content", changeId,
        changeInfo.currentRevision, filePath.replace("/", "%2F"));
    var mockJsonElement = Mockito.mock(JsonElement.class);
    var fileContentAsString = RandomString.make();
    Mockito.when(gerritRestClient.getRequest(request)).thenReturn(mockJsonElement);
    Mockito.when(mockJsonElement.getAsString()).thenReturn(fileContentAsString);

    var fileContent = gerritService.getFileContent(changeId, filePath);

    Assertions.assertThat(fileContent).isNotNull();
    Assertions.assertThat(fileContent).isEqualTo(fileContentAsString);
    Assertions.assertThatCode(
        () -> gerritService.getFileContent(changeId, filePath))
        .doesNotThrowAnyException();
  }

  @Test
  @SneakyThrows
  void getFileContentHttpExceptionTest() {
    var changeId = RandomString.make();
    var filePath = RandomString.make();
    Mockito.when(gerritApiImpl.changes()).thenReturn(changes);
    Mockito.when(changes.id(changeId)).thenThrow(HttpStatusException.class);

    Assertions.assertThatCode(() -> gerritService.getFileContent(changeId, filePath))
        .isInstanceOf(GerritCommunicationException.class);
  }

  @Test
  @SneakyThrows
  void getFileContentNotFoundExceptionTest() {
    var changeId = RandomString.make();
    var filePath = RandomString.make();
    Mockito.when(gerritApiImpl.changes()).thenReturn(changes);
    Mockito.when(changes.id(changeId)).thenThrow(
        new HttpStatusException(HttpStatus.NOT_FOUND.value(), "", ""));

    Assertions.assertThatCode(() -> gerritService.getFileContent(changeId, filePath))
        .isInstanceOf(GerritChangeNotFoundException.class);
  }

  @Test
  @SneakyThrows
  void getFileContentRestApiExceptionTest() {
    var changeId = RandomString.make();
    var filePath = RandomString.make();
    Mockito.when(gerritApiImpl.changes()).thenReturn(changes);
    Mockito.when(changes.id(changeId)).thenThrow(RestApiException.class);

    Assertions.assertThatCode(() -> gerritService.getFileContent(changeId, filePath))
        .isInstanceOf(GerritCommunicationException.class);
  }
}
