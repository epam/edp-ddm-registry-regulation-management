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

package com.epam.digital.data.platform.management.core.event.publisher;

import com.epam.digital.data.platform.management.core.event.VersionCandidateCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Class that is used for publishing application events such as:
 * <li>{@link VersionCandidateCreatedEvent Version candindate created event}</li>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistryRegulationManagementEventPublisher {

  private final ApplicationEventPublisher applicationEventPublisher;

  public void publishVersionCandidateCreatedEvent(String versionCandidateNumber) {
    log.debug("Publish version candidate {} created event", versionCandidateNumber);
    var event = new VersionCandidateCreatedEvent(this, versionCandidateNumber);
    applicationEventPublisher.publishEvent(event);
  }
}
