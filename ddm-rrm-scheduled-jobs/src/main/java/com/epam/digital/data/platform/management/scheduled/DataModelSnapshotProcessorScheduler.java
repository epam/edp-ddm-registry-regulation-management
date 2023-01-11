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
package com.epam.digital.data.platform.management.scheduled;

import data.model.snapshot.DdmSchemaProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class DataModelSnapshotProcessorScheduler {

  private final DdmSchemaProcessor ddmSchemaProcessor;

  @Scheduled(cron = "${scheduled.dataModelSnapshotCron:-}", zone = "${scheduled.dataModelSnapshotTimeZone:UTC}")
  public void process() {
    try {
      ddmSchemaProcessor.run();
    } catch (Exception e) {
      log.warn("Couldn't read database schema: {}", e.getMessage(), e);
    }
  }

}
