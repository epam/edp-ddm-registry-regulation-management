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

import static org.mockito.ArgumentMatchers.refEq;

import com.epam.digital.data.platform.management.gerritintegration.exception.GerritChangeNotFoundException;
import com.epam.digital.data.platform.management.gerritintegration.exception.GerritCommunicationException;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.api.changes.ReviewResult;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.urswolfer.gerrit.client.rest.http.HttpStatusException;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(SpringExtension.class)
public class GerritServiceReviewTest extends AbstractGerritServiceTest {

  @Captor
  private ArgumentCaptor<ReviewInput> captor;

  @Test
  @SneakyThrows
  void reviewTest() {
    var reviewResult = new ReviewResult();
    var changeId = RandomString.make();
    reviewResult.ready = true;
    Mockito.when(changes.id(changeId)).thenReturn(changeApiRestClient);
    Mockito.when(changeApiRestClient.current()).thenReturn(revisionApi);
    Mockito.when(revisionApi.review(captor.capture())).thenReturn(reviewResult);
    Boolean review = gerritService.review(changeId);

    Mockito.verify(revisionApi, Mockito.times(1)).review(captor.getValue());
    Assertions.assertThatCode(() ->gerritService.review(changeId))
        .doesNotThrowAnyException();
    Assertions.assertThat(review).isNotNull();
  }

  @Test
  @SneakyThrows
  void notFoundExceptionTest() {

    Mockito.when(changes.id("changeId")).thenThrow(
        new HttpStatusException(HttpStatus.NOT_FOUND.value(), "", ""));
    Assertions.assertThatCode(() -> gerritService.review("changeId"))
        .isInstanceOf(GerritChangeNotFoundException.class);
    Mockito.verify(revisionApi, Mockito.never()).review(refEq(new ReviewInput()));
  }

  @Test
  @SneakyThrows
  void httpExceptionTest() {

    Mockito.when(changes.id("changeId")).thenThrow(HttpStatusException.class);

    Assertions.assertThatCode(() -> gerritService.review("changeId"))
        .isInstanceOf(GerritCommunicationException.class);
    Mockito.verify(revisionApi, Mockito.never()).review(new ReviewInput());
  }

  @Test
  @SneakyThrows
  void restApiExceptionTest() {

    Mockito.when(changes.id("changeId")).thenThrow(RestApiException.class);

    Assertions.assertThatCode(() -> gerritService.review("changeId"))
        .isInstanceOf(GerritCommunicationException.class);
    Mockito.verify(revisionApi, Mockito.never()).review(new ReviewInput());
  }
}
