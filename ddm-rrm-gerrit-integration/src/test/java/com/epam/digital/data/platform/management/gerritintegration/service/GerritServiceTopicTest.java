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
import com.google.gerrit.extensions.api.changes.ChangeApi;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.urswolfer.gerrit.client.rest.http.HttpStatusException;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(SpringExtension.class)
public class GerritServiceTopicTest extends AbstractGerritServiceTest {

  @Test
  @SneakyThrows
  void getTopicFromChange() {
    var changeId = RandomString.make();
    var expected = RandomString.make();
    var changeApi = Mockito.mock(ChangeApi.class);
    Mockito.when(changes.id(changeId)).thenReturn(changeApi);
    Mockito.when(changeApi.topic()).thenReturn(expected);
    var actual = gerritService.getTopic(changeId);
    Assertions.assertThat(actual).isEqualTo(expected);
  }

  @Test
  @SneakyThrows
  void writeAndGetTopicFromChangeTest() {
    var topic1 = RandomString.make();
    var topic2 = RandomString.make();

    var changeApi = Mockito.mock(ChangeApi.class);
    Mockito.when(changes.id(any())).thenReturn(changeApi);

    ArgumentCaptor<String> valueCapture = ArgumentCaptor.forClass(String.class);
    Mockito.when(changeApi.topic()).thenAnswer(e -> valueCapture.getValue());
    Mockito.doNothing().when(changeApi).topic(valueCapture.capture());

    gerritService.setTopic(topic1, "");
    Assertions.assertThat(valueCapture.getValue()).isEqualTo(topic1);
    var rTopic1 = gerritService.getTopic("");
    gerritService.setTopic(topic2, "");
    Assertions.assertThat(valueCapture.getValue()).isEqualTo(topic2);
    var rTopic2 = gerritService.getTopic("");

    Assertions.assertThat(rTopic1).isEqualTo(topic1);
    Assertions.assertThat(rTopic2).isEqualTo(topic2);
  }

  @Test
  @SneakyThrows
  void topicHttpException() {
    var changeId = RandomString.make();
    var topic = RandomString.make();
    Mockito.doThrow(HttpStatusException.class).when(changes).id(changeId);

    Assertions.assertThatCode(
            () -> gerritService.getTopic(changeId))
        .isInstanceOf(GerritCommunicationException.class);

    Assertions.assertThatCode(
            () -> gerritService.setTopic(topic, changeId))
        .isInstanceOf(GerritCommunicationException.class);

  }

  @Test
  @SneakyThrows
  void topicRestApiException() {
    var changeId = RandomString.make();
    var topic = RandomString.make();
    Mockito.doThrow(RestApiException.class).when(changes).id(changeId);

    Assertions.assertThatCode(
            () -> gerritService.getTopic(changeId))
        .isInstanceOf(GerritCommunicationException.class);

    Assertions.assertThatCode(
            () -> gerritService.setTopic(topic, changeId))
        .isInstanceOf(GerritCommunicationException.class);
  }

  @Test
  @SneakyThrows
  void topicNotFoundExceptionTest() {
    var changeId = RandomString.make();
    var topic = RandomString.make();
    Mockito.doThrow(new HttpStatusException(HttpStatus.NOT_FOUND.value(), "", ""))
        .when(changes).id(changeId);

    Assertions.assertThatCode(
            () -> gerritService.getTopic(changeId))
        .isInstanceOf(GerritChangeNotFoundException.class);

    Assertions.assertThatCode(
            () -> gerritService.setTopic(topic, changeId))
        .isInstanceOf(GerritChangeNotFoundException.class);
  }
}
