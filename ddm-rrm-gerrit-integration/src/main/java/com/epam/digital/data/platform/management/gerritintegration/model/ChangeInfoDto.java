/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.digital.data.platform.management.gerritintegration.model;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeInfoDto {

  private static final int NUMBER_SIZE = 4;

  private String changeId;
  private String refs;
  private String number;
  private String subject;
  private String description;
  private LocalDateTime created;
  private LocalDateTime updated;
  private LocalDateTime submitted;
  private String id;
  private String project;
  private String branch;
  private String owner;
  private String topic;
  private Boolean mergeable;
  private Map<String, Boolean> labels;
}
