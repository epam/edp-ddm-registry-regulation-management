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

import io.swagger.v3.oas.annotations.media.Schema;
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
public class MasterVersionInfoDetailed {
  /**
   * last MR number
   */
  @Schema(description = "Last version candidate identifier")
  private String id;
  /**
   * last MR name
   */
  @Schema(description = "Last version candidate name")
  private String name;
  /**
   * last MR description
   */
  @Schema(description = "Last version candidate description")
  private String description;
  /**
   * last MR autor
   */
  @Schema(description = "Last version candidate author")
  private String author;
  /**
   * last MR merge time
   */
  @Schema(description = "Last version candidate update time")
  private LocalDateTime latestUpdate;
  /**
   * todo this field can be retrieved only from jenkins
   */
  @Schema(description = "Last version candidate publication flag")
  private Boolean published;
  /**
   * last MR reviewer
   */
  @Schema(description = "Last version candidate inspector")
  private String inspector;
  /**
   * last MR validations
   */
  @Schema(description = "Last version candidate validations")
  private List<Validation> validations;
}
