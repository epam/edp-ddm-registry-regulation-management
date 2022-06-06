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

package com.epam.digital.data.platform.management.mapper;

import com.epam.digital.data.platform.management.filemanagement.model.FileStatus;
import com.epam.digital.data.platform.management.filemanagement.model.VersionedFileInfoDto;
import com.epam.digital.data.platform.management.model.dto.DataModelFileDto;
import com.epam.digital.data.platform.management.model.dto.DataModelFileStatus;
import com.epam.digital.data.platform.management.model.dto.DataModelFileType;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;

/**
 * Mapper that is used for mapping from file-management dtos to data-model-management dtos
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface DataModelFileManagementMapper {

  @Mapping(source = "fileDto.name", target = "fileName")
  @Mapping(source = "fileDto.name", target = "type")
  DataModelFileDto toChangedDataModelFileDto(VersionedFileInfoDto fileDto, boolean conflicted);

  @ValueMapping(source = "DELETED", target = MappingConstants.NULL)
  DataModelFileStatus toDataModelFileStatus(FileStatus status);

  default DataModelFileType getFileType(String fileName) {
    return DataModelFileType.getByFileName(fileName);
  }
}
