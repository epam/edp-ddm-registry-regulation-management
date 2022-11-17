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
import com.epam.digital.data.platform.management.gerritintegration.exception.GerritConflictException;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.urswolfer.gerrit.client.rest.http.HttpStatusException;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(SpringExtension.class)
public class GerritServiceSubmitTest extends AbstractGerritServiceTest {
  @Test
  @SneakyThrows
  void submitTest() {
    var changeId = RandomString.make();
    Mockito.when(changes.id(changeId)).thenReturn(changeApiRestClient);
    Mockito.when(changeApiRestClient.current()).thenReturn(revisionApi);

    Assertions.assertThatCode(() -> gerritService.submitChanges(changeId))
        .doesNotThrowAnyException();

    Mockito.verify(revisionApi, Mockito.times(1)).submit();
  }

  @Test
  @SneakyThrows
  void submitConflictExceptionTest() {
    var changeId = RandomString.make();
    Mockito.when(changes.id(changeId)).thenThrow(
        new HttpStatusException(HttpStatus.CONFLICT.value(), "", ""));

    Assertions.assertThatCode(() -> gerritService.submitChanges(changeId))
        .isInstanceOf(GerritConflictException.class);

    Mockito.verify(revisionApi, Mockito.never()).submit();
  }

  @Test
  @SneakyThrows
  void submitNotFoundExceptionTest() {
    var changeId = RandomString.make();
    Mockito.when(changes.id(changeId)).thenThrow(
        new HttpStatusException(HttpStatus.NOT_FOUND.value(), "", ""));

    Assertions.assertThatCode(() -> gerritService.submitChanges(changeId))
        .isInstanceOf(GerritChangeNotFoundException.class);

    Mockito.verify(revisionApi, Mockito.never()).submit();
  }

  @Test
  @SneakyThrows
  void submitHttpExceptionTest() {
    var changeId = RandomString.make();
    Mockito.when(changes.id(changeId)).thenThrow(HttpStatusException.class);

    Assertions.assertThatCode(() -> gerritService.submitChanges(changeId))
        .isInstanceOf(GerritCommunicationException.class);

    Mockito.verify(revisionApi, Mockito.never()).submit();
  }

  @Test
  @SneakyThrows
  void submitRestApiExceptionTest() {
    var changeId = RandomString.make();
    Mockito.when(changes.id(changeId)).thenThrow(RestApiException.class);

    Assertions.assertThatCode(() -> gerritService.submitChanges(changeId))
        .isInstanceOf(GerritCommunicationException.class);

    Mockito.verify(revisionApi, Mockito.never()).submit();
  }
}
