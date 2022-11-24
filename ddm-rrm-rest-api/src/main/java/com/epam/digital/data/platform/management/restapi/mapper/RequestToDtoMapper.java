package com.epam.digital.data.platform.management.restapi.mapper;

import com.epam.digital.data.platform.management.gerritintegration.model.CreateChangeInputDto;
import com.epam.digital.data.platform.management.restapi.model.CreateVersionRequest;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface RequestToDtoMapper {

  CreateChangeInputDto toDto(CreateVersionRequest request);
}
