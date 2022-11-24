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

import static org.mockito.ArgumentMatchers.eq;

import com.epam.digital.data.platform.management.gerritintegration.exception.GerritChangeNotFoundException;
import com.epam.digital.data.platform.management.gerritintegration.exception.GerritCommunicationException;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.urswolfer.gerrit.client.rest.http.HttpStatusException;
import java.util.ArrayList;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(SpringExtension.class)
public class GerritServiceClosedMrTest extends AbstractGerritServiceTest {

  @Test
  @SneakyThrows
  void getClosedMrIdsTest() {
    var repo = RandomString.make();
    var user = RandomString.make();
    var query = String.format("project:%s+status:closed+owner:%s", repo, user);

    Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn(repo);
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn(user);
    Mockito.when(changes.query(eq(query))).thenReturn(request);
    Mockito.when(request.get()).thenReturn(new ArrayList<>());

    var closedMrIds = gerritService.getClosedMrIds();
    Mockito.verify(changes).query(query);
    Mockito.verify(request).get();
    Assertions.assertThat(closedMrIds).isNotNull();
  }

  @Test
  @SneakyThrows
  void getClosedMrIdsNotFoundTest() {
    var repo = RandomString.make();
    var user = RandomString.make();
    var query = String.format("project:%s+status:closed+owner:%s", repo, user);

    Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn(repo);
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn(user);
    Mockito.when(changes.query(eq(query))).thenReturn(request);
    Mockito.when(request.get()).thenThrow(
        new HttpStatusException(HttpStatus.NOT_FOUND.value(), "", ""));

    Assertions.assertThatCode(() -> gerritService.getClosedMrIds())
        .isInstanceOf(GerritChangeNotFoundException.class);
  }

  @Test
  @SneakyThrows
  void getClosedMrIdsHttpExceptionTest() {
    var repo = RandomString.make();
    var user = RandomString.make();
    var query = String.format("project:%s+status:closed+owner:%s", repo, user);

    Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn(repo);
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn(user);
    Mockito.when(changes.query(eq(query))).thenReturn(request);
    Mockito.when(request.get()).thenThrow(HttpStatusException.class);

    Assertions.assertThatCode(() -> gerritService.getClosedMrIds())
        .isInstanceOf(GerritCommunicationException.class);
  }

  @Test
  @SneakyThrows
  void getClosedMrIdsRestApiExceptionTest() {
    var repo = RandomString.make();
    var user = RandomString.make();
    var query = String.format("project:%s+status:closed+owner:%s", repo, user);

    Mockito.when(gerritPropertiesConfig.getRepository()).thenReturn(repo);
    Mockito.when(gerritPropertiesConfig.getUser()).thenReturn(user);
    Mockito.when(changes.query(eq(query))).thenReturn(request);
    Mockito.when(request.get()).thenThrow(RestApiException.class);

    Assertions.assertThatCode(() -> gerritService.getClosedMrIds())
        .isInstanceOf(GerritCommunicationException.class);
  }
}
