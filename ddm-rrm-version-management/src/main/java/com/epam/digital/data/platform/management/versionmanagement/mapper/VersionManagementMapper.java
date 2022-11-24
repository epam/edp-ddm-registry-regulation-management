package com.epam.digital.data.platform.management.versionmanagement.mapper;

import com.epam.digital.data.platform.management.forms.model.FormInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.model.ChangeInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.model.FileInfoDto;
import com.epam.digital.data.platform.management.model.dto.BusinessProcessInfoDto;
import com.epam.digital.data.platform.management.versionmanagement.model.EntityChangesInfoDto;
import com.epam.digital.data.platform.management.versionmanagement.model.VersionInfoDto;
import com.epam.digital.data.platform.management.versionmanagement.model.VersionedFileInfoDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Maps external dtos to current module dtos
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface VersionManagementMapper {

  EntityChangesInfoDto bpInfoDtoToChangeInfo(BusinessProcessInfoDto dto);
  EntityChangesInfoDto formInfoDtoToChangeInfo(FormInfoDto dto);

  @Mapping(target = "description", source = "topic")
  VersionInfoDto toVersionInfoDto(ChangeInfoDto changeInfoDto);

  @Mapping(target = "name", source = "fileName")
  @Mapping(target = "status", source = "fileInfoDto.status")
  @Mapping(target = "lineInserted", source = "fileInfoDto.linesInserted")
  @Mapping(target = "lineDeleted", source = "fileInfoDto.linesDeleted")
  @Mapping(target = "size", source = "fileInfoDto.size")
  @Mapping(target = "sizeDelta", source = "fileInfoDto.sizeDelta")
  VersionedFileInfoDto toVersionedFileInfoDto(String fileName, FileInfoDto fileInfoDto);

}
