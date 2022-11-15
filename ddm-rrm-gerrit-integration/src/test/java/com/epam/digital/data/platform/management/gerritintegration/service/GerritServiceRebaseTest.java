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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.epam.digital.data.platform.management.gerritintegration.exception.GerritCommunicationException;
import com.google.gerrit.extensions.api.changes.RebaseInput;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gson.Gson;
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
public class GerritServiceRebaseTest extends AbstractGerritServiceTest {

  @Test
  @SneakyThrows
  void rebaseTest() {
    var changeId = RandomString.make();
    var request = String.format("/changes/%s/rebase", changeId);
    Mockito.when(gerritApiImpl.restClient()).thenReturn(gerritRestClient);
    var requestBody = new Gson().toJson(new RebaseInput(), RebaseInput.class);
    Mockito.when(gerritRestClient.postRequest(request, requestBody)).thenReturn(null);

    Assertions.assertThatCode(() -> gerritService.rebase(changeId))
        .doesNotThrowAnyException();

    Mockito.verify(gerritRestClient).postRequest(request, requestBody);
  }

  @Test
  @SneakyThrows
  void rebaseConflictSkipTest() {
    var changeId = RandomString.make();
    var request = String.format("/changes/%s/rebase", changeId);
    Mockito.when(gerritApiImpl.restClient()).thenReturn(gerritRestClient);
    var requestBody = new Gson().toJson(new RebaseInput(), RebaseInput.class);
    Mockito.when(gerritRestClient.postRequest(request, requestBody)).thenThrow(
        new HttpStatusException(HttpStatus.CONFLICT.value(), "", ""));

    Assertions.assertThatCode(() -> gerritService.rebase(changeId))
        .doesNotThrowAnyException();

    Mockito.verify(gerritRestClient).postRequest(request, requestBody);
  }

  @Test
  @SneakyThrows
  void rebaseHttpExceptionTest() {
    var changeId = RandomString.make();
    var request = String.format("/changes/%s/rebase", changeId);
    Mockito.when(gerritApiImpl.restClient()).thenReturn(gerritRestClient);
    var requestBody = new Gson().toJson(new RebaseInput(), RebaseInput.class);
    Mockito.when(gerritRestClient.postRequest(request, requestBody)).thenThrow(
        HttpStatusException.class);

    Assertions.assertThatCode(() -> gerritService.rebase(changeId))
        .isInstanceOf(GerritCommunicationException.class);

    Mockito.verify(gerritRestClient).postRequest(request, requestBody);
  }

  @Test
  @SneakyThrows
  void rebaseRestApiExceptionTest() {
    var changeId = RandomString.make();
    var request = String.format("/changes/%s/rebase", changeId);
    Mockito.when(gerritApiImpl.restClient()).thenReturn(gerritRestClient);
    var requestBody = new Gson().toJson(new RebaseInput(), RebaseInput.class);
    Mockito.when(gerritRestClient.postRequest(request, requestBody)).thenThrow(
        RestApiException.class);

    Assertions.assertThatCode(() -> gerritService.rebase(changeId))
        .isInstanceOf(GerritCommunicationException.class);

    Mockito.verify(gerritRestClient).postRequest(request, requestBody);
  }

  @Test
  @SneakyThrows
  void rebaseNullTest() {
    var changeId = RandomString.make();
    var request = String.format("/changes/%s/rebase", changeId);
    Assertions.assertThatCode(() -> gerritService.rebase(null)).doesNotThrowAnyException();
    Mockito.verify(gerritRestClient, Mockito.never()).postRequest(eq(request), any());
  }

}
