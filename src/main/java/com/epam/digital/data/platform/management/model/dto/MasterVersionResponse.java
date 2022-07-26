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
package com.epam.digital.data.platform.management.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;


@Getter
@Setter
@Builder
@AllArgsConstructor
//todo rename to sync with VersionInfoDetailed
public class MasterVersionResponse {
  /**
   * last MR number
   */
  private String id;
  /**
   * last MR name
   */
  private String name;
  /**
   * last MR description
   */
  private String description;
  /**
   * last MR autor
   */
  private String author;
  /**
   * last MR merge time
   */
  private LocalDateTime latestUpdate;
  /**
   * todo this field can be retrieved only from jenkins
   */
  private Boolean published;
  /**
   * last MR reviewer
   */
  private String inspector;
  /**
   * last MR validations
   */
  private List<Validation> validations;
}
