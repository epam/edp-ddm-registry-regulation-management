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

import com.epam.digital.data.platform.management.gerritintegration.exception.GerritChangeNotFoundException;
import com.epam.digital.data.platform.management.gerritintegration.exception.GerritCommunicationException;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.urswolfer.gerrit.client.rest.http.HttpStatusException;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(SpringExtension.class)
public class GerritServiceDeclineTest extends AbstractGerritServiceTest {

  @Test
  @SneakyThrows
  void declineChangeTest() {
    Mockito.when(changes.id(any())).thenReturn(changeApiRestClient);
    Assertions.assertThatCode(() -> gerritService.declineChange("changeId"))
        .doesNotThrowAnyException();
    Mockito.verify(changeApiRestClient, Mockito.times(1)).abandon();
  }

  @Test
  @SneakyThrows
  void declineChangeNotFoundExceptionTest() {
    Mockito.when(changes.id(any())).thenThrow(
        new HttpStatusException(HttpStatus.NOT_FOUND.value(), "", ""));

    Assertions.assertThatCode(() -> gerritService.declineChange("changeId"))
        .isInstanceOf(GerritChangeNotFoundException.class);

    Mockito.verify(changeApiRestClient, Mockito.never()).abandon();
  }

  @Test
  @SneakyThrows
  void declineChangeHttpExceptionTest() {
    Mockito.when(changes.id(any())).thenThrow(HttpStatusException.class);

    Assertions.assertThatCode(() -> gerritService.declineChange("changeId"))
        .isInstanceOf(GerritCommunicationException.class);
    Mockito.verify(changeApiRestClient, Mockito.never()).abandon();
  }

  @Test
  @SneakyThrows
  void declineChangeRestApiExceptionTest() {
    Mockito.when(changes.id(any())).thenThrow(RestApiException.class);

    Assertions.assertThatCode(() -> gerritService.declineChange("changeId"))
        .isInstanceOf(GerritCommunicationException.class);
    Mockito.verify(changeApiRestClient, Mockito.never()).abandon();
  }
}
