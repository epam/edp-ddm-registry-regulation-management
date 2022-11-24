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

package com.epam.digital.data.platform.management.core.event;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

/**
 * Provide handler for version candidate creation event
 */
public interface VersionCandidateCreatedEventListener {

  /**
   * Handle version candidate creation event
   * @param event {@link VersionCandidateCreatedEvent}
   */
  @Async
  @EventListener(value = VersionCandidateCreatedEvent.class)
  void handleVersionCandidateCreatedEvent(VersionCandidateCreatedEvent event);

}
