/*
 * Copyright 2023 EPAM Systems.
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
package com.epam.digital.data.platform.management.versionmanagement.model;

import com.google.gerrit.extensions.common.ChangeMessageInfo;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
public class VersionInfoDto {

  private String subject;
  private String description;
  private LocalDateTime created;
  private LocalDateTime updated;
  private LocalDateTime rebased;
  private LocalDateTime submitted;
  private String changeId;
  private String id;
  private int number;
  private String project;
  private String branch;
  private String owner;
  private Boolean mergeable;
  private Map<String, Integer> labels;
  private List<ChangeMessageInfo> messages;
}
