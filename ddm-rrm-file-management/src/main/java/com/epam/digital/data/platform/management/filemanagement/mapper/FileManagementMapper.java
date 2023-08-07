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

package com.epam.digital.data.platform.management.filemanagement.mapper;

import com.epam.digital.data.platform.management.filemanagement.model.VersionedFileDatesDto;
import com.epam.digital.data.platform.management.filemanagement.model.VersionedFileInfoDto;
import com.epam.digital.data.platform.management.gitintegration.model.FileDatesDto;
import org.apache.commons.io.FilenameUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

/**
 * Map Gerrit and Git dtos to file-management dtos
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface FileManagementMapper {

  @Mapping(target = "name", source = "filePath", qualifiedByName = "pathToName")
  @Mapping(target = "path", source = "filePath")
  @Mapping(target = "status", constant = "UNCHANGED")
  VersionedFileInfoDto toVersionedFileInfoDto(String filePath);

  @Named("pathToName")
  default String pathToName(String filePath) {
    return FilenameUtils.getBaseName(filePath);
  }


  @Mapping(target = "created", source = "create")
  @Mapping(target = "updated", source = "update")
  VersionedFileDatesDto toVersionedFileDatesDto(FileDatesDto fileDatesDto);

}
