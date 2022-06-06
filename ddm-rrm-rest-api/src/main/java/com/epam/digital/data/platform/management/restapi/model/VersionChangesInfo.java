/*
 * Copyright 2023 EPAM Systems.
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

package com.epam.digital.data.platform.management.restapi.model;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
@EqualsAndHashCode
public class VersionChangesInfo {

  @Schema(required = true, description = "List of changed forms")
  private List<EntityChangesInfo> changedForms;
  @Schema(required = true, description = "List of changed business processes")
  private List<EntityChangesInfo> changedBusinessProcesses;
  @Schema(required = true, description = "List of changed data-model files")
  private List<DataModelChangesInfo> changedDataModelFiles;
  @Schema(required = true, description = "List of changed groups")
  private List<EntityChangesInfo> changedGroups;

  @Builder
  @Getter
  @EqualsAndHashCode
  public static class EntityChangesInfo {

    @Schema(required = true, description = "Changed entity name")
    private String name;
    @Schema(required = true, description = "Changed entity title")
    private String title;
    @Schema(required = true, description = "Entity status. It's NEW, CHANGED or DELETED")
    private ChangedFileStatus status;
    @Schema(nullable = true, description = "Is entity has conflicts")
    private Boolean conflicted;

    public enum ChangedFileStatus {
      NEW,
      CHANGED,
      DELETED
    }
  }

  @Builder
  @Getter
  @EqualsAndHashCode
  public static class DataModelChangesInfo {

    @Schema(required = true, description = "Data model file name")
    private String name;
    @Schema(description = "Data model file type.")
    private DataModelFileType fileType;
    @Schema(description = "Data model file status. It's NEW or CHANGED")
    private DataModelFileStatus status;
    @Schema(nullable = true, description = "Is data model has conflicts")
    private Boolean conflicted;

    public enum DataModelFileType {
      TABLES_FILE
    }

    public enum DataModelFileStatus {
      NEW,
      CHANGED
    }
  }
}
