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
import com.epam.digital.data.platform.management.gerritintegration.model.CreateChangeInputDto;
import com.google.gerrit.extensions.restapi.RestApiException;
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
public class GerritServiceCreateChangesTest extends AbstractGerritServiceTest {

  @Test
  @SneakyThrows
  void createChangesTest() {
    Mockito.when(changes.create(any())).thenReturn(changeApiRestClient);
    Mockito.when(changeApiRestClient.get()).thenReturn(changeInfo);

    var name = RandomString.make();
    var description = RandomString.make();

    var request = CreateChangeInputDto.builder()
        .name(name).description(description).build();

    var change = gerritService.createChanges(request);
    Assertions.assertThat(change).isNotNull();
    Assertions.assertThat(change).isEqualTo("5");
    Mockito.verify(changes, Mockito.times(1)).create(any());

    Assertions.assertThatCode(() -> gerritService.createChanges(request))
        .doesNotThrowAnyException();
  }


  @Test
  @SneakyThrows
  void notFoundExceptionTest() {
    var name = RandomString.make();
    var description = RandomString.make();

    var request = CreateChangeInputDto.builder()
        .name(name).description(description).build();
    Mockito.when(changes.create(any())).thenThrow(
        new HttpStatusException(HttpStatus.NOT_FOUND.value(), "", ""));

    Assertions.assertThatCode(() -> gerritService.createChanges(request))
        .isInstanceOf(GerritChangeNotFoundException.class);
    Mockito.verify(changeApiRestClient, Mockito.never()).get();
  }

  @Test
  @SneakyThrows
  void httpExceptionTest() {
    var name = RandomString.make();
    var description = RandomString.make();

    var request = CreateChangeInputDto.builder()
        .name(name).description(description).build();
    Mockito.when(changes.create(any())).thenThrow(HttpStatusException.class);

    Assertions.assertThatCode(() -> gerritService.createChanges(request))
        .isInstanceOf(GerritCommunicationException.class);
    Mockito.verify(changeApiRestClient, Mockito.never()).get();
  }

  @Test
  @SneakyThrows
  void restApiExceptionTest() {

    var name = RandomString.make();
    var description = RandomString.make();

    var request = CreateChangeInputDto.builder()
        .name(name).description(description).build();
    Mockito.when(changes.create(any())).thenThrow(RestApiException.class);

    Assertions.assertThatCode(() -> gerritService.createChanges(request))
        .isInstanceOf(GerritCommunicationException.class);
    Mockito.verify(changeApiRestClient, Mockito.never()).get();
  }
}
