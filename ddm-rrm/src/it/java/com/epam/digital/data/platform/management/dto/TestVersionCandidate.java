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

package com.epam.digital.data.platform.management.dto;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Random;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.assertj.core.internal.bytebuddy.utility.RandomString;

@Getter
public class TestVersionCandidate {

  @Setter
  private String id;
  @Setter
  private String changeId;
  private final int number;

  private final String subject;
  private final String topic;
  private final boolean mergeable;
  private final LocalDateTime created;
  private final LocalDateTime updated;

  private final String currentRevision;
  private final String ref;

  @Builder
  private TestVersionCandidate(String subject, String topic, boolean mergeable,
      LocalDateTime created, LocalDateTime updated) {
    this.number = new Random().nextInt(Integer.MAX_VALUE);
    this.subject = Objects.isNull(subject) ? RandomString.make() : subject;
    this.topic = Objects.isNull(topic) ? RandomString.make() : topic;
    this.mergeable = mergeable;
    this.created = Objects.isNull(created) ? LocalDateTime.now() : created;
    this.updated = Objects.isNull(updated) ? LocalDateTime.now() : updated;
    this.currentRevision = RandomString.make();
    this.ref = RandomString.make();
  }
}
