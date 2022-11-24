package com.epam.digital.data.platform.management.gerritintegration.mapper;

import com.epam.digital.data.platform.management.gerritintegration.model.ChangeInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.model.FileInfoDto;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.FileInfo;
import com.google.gerrit.extensions.common.LabelInfo;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

/**
 * Map Gerrit models to DTOs
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface GerritMapper {

  Map<String, FileInfoDto> toFileDto(Map<String, FileInfo> dtoMap);

  @Mapping(target = "number", source = "_number")
  @Mapping(target = "created", qualifiedByName = "toLocalDateTime")
  @Mapping(target = "updated", qualifiedByName = "toLocalDateTime")
  @Mapping(target = "submitted", qualifiedByName = "toLocalDateTime")
  @Mapping(target = "owner", source = "owner.username")
  ChangeInfoDto toChangeInfoDto(ChangeInfo changeInfo);

  default Map<String,Boolean> toLabelMap(Map<String, LabelInfo> labels) {
    if (labels == null) {
      return null;
    }
    return labels.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().approved != null));
  }

  @Named("toLocalDateTime")
  default LocalDateTime toLocalDateTime(Timestamp timestamp) {
    if (Objects.isNull(timestamp)) {
      return null;
    }
    return LocalDateTime.ofInstant(timestamp.toInstant(), ZoneId.of("UTC"));
  }
}
