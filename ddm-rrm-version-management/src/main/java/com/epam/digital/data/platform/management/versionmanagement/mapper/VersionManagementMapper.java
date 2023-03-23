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
package com.epam.digital.data.platform.management.versionmanagement.mapper;

import com.epam.digital.data.platform.management.filemanagement.model.FileStatus;
import com.epam.digital.data.platform.management.forms.model.FormInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.model.ChangeInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.model.ChangeInfoShortDto;
import com.epam.digital.data.platform.management.gerritintegration.model.FileInfoDto;
import com.epam.digital.data.platform.management.groups.model.GroupChangesDetails;
import com.epam.digital.data.platform.management.model.dto.BusinessProcessInfoDto;
import com.epam.digital.data.platform.management.model.dto.DataModelFileDto;
import com.epam.digital.data.platform.management.model.dto.DataModelFileStatus;
import com.epam.digital.data.platform.management.versionmanagement.model.DataModelChangesInfoDto;
import com.epam.digital.data.platform.management.versionmanagement.model.EntityChangesInfoDto;
import com.epam.digital.data.platform.management.versionmanagement.model.EntityChangesInfoDto.ChangedFileStatus;
import com.epam.digital.data.platform.management.versionmanagement.model.VersionInfoDto;
import com.epam.digital.data.platform.management.versionmanagement.model.VersionInfoShortDto;
import com.epam.digital.data.platform.management.versionmanagement.model.VersionedFileInfoDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;

/**
 * Maps external dtos to current module dtos
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface VersionManagementMapper {

  EntityChangesInfoDto bpInfoDtoToChangeInfo(BusinessProcessInfoDto dto);

  EntityChangesInfoDto formInfoDtoToChangeInfo(FormInfoDto dto);

  EntityChangesInfoDto groupingToChangeInfo(GroupChangesDetails dto);

  @ValueMapping(source = "UNCHANGED", target = MappingConstants.NULL)
  ChangedFileStatus toChangedFileStatus(FileStatus status);

  @Mapping(target = "description", source = "topic")
  VersionInfoShortDto toVersionInfoDto(ChangeInfoShortDto changeInfoDto);

  @Mapping(target = "description", source = "topic")
  VersionInfoDto toVersionInfoDto(ChangeInfoDto changeInfoDto);

  @Mapping(target = "name", source = "fileName")
  @Mapping(target = "status", source = "fileInfoDto.status")
  @Mapping(target = "lineInserted", source = "fileInfoDto.linesInserted")
  @Mapping(target = "lineDeleted", source = "fileInfoDto.linesDeleted")
  @Mapping(target = "size", source = "fileInfoDto.size")
  @Mapping(target = "sizeDelta", source = "fileInfoDto.sizeDelta")
  VersionedFileInfoDto toVersionedFileInfoDto(String fileName, FileInfoDto fileInfoDto);

  @Mapping(source = "fileName", target = "name")
  @Mapping(source = "type", target = "fileType")
  DataModelChangesInfoDto toDataModelChangesInfoDto(DataModelFileDto dto);

  @ValueMapping(source = "UNCHANGED", target = MappingConstants.NULL)
  DataModelChangesInfoDto.DataModelFileStatus mapDataModelFileStatus(DataModelFileStatus status);

}
