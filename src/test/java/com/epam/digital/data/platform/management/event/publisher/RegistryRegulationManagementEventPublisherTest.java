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

package com.epam.digital.data.platform.management.event.publisher;

import com.epam.digital.data.platform.management.event.VersionCandidateCreatedEvent;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class RegistryRegulationManagementEventPublisherTest {

  @InjectMocks
  RegistryRegulationManagementEventPublisher publisher;

  @Mock
  ApplicationEventPublisher applicationEventPublisher;

  @Captor
  ArgumentCaptor<VersionCandidateCreatedEvent> versionCandidateCreatedEventArgumentCaptor;

  @Test
  void publishVersionCandidateCreatedEvent() {
    final var versionCandidateNumber = RandomString.make();

    publisher.publishVersionCandidateCreatedEvent(versionCandidateNumber);

    Mockito.verify(applicationEventPublisher)
        .publishEvent(versionCandidateCreatedEventArgumentCaptor.capture());

    final var actualEvent = versionCandidateCreatedEventArgumentCaptor.getValue();

    Assertions.assertThat(actualEvent)
        .hasFieldOrPropertyWithValue("source", publisher)
        .hasFieldOrPropertyWithValue("versionCandidateNumber", versionCandidateNumber);
  }
}
