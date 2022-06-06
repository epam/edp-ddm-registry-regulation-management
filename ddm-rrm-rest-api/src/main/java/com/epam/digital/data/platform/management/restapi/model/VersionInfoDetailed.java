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
package com.epam.digital.data.platform.management.restapi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class VersionInfoDetailed {
    @Schema(required = true, description = "Version candidate identifier")
    private String id;
    @Schema(required = true, description = "Version candidate name")
    private String name;
    @Schema(description = "Version candidate description")
    private String description;
    @Schema(required = true, description = "Version candidate author")
    private String author;
    @Schema(required = true, description = "Version candidate creation time")
    private LocalDateTime creationDate;
    @Schema(description = "Version candidate update time")
    private LocalDateTime latestUpdate;
    @Schema(description = "Version candidate last rebase time")
    private LocalDateTime latestRebase;
    @Schema(required = true, description = "Version candidate conflicts flag")
    private Boolean hasConflicts;
    @Schema(description = "Version candidate inspections")
    private List<Inspection> inspections;
    @Schema(description = "Version candidate validations")
    private List<Validation> validations;
}
