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

  default Map<String, Integer> toLabelMap(Map<String, LabelInfo> labels) {
    if (labels == null) {
      return null;
    }
    return labels.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> toLabelValue(e.getValue())));
  }

  @Named("toLocalDateTime")
  default LocalDateTime toLocalDateTime(Timestamp timestamp) {
    if (Objects.isNull(timestamp)) {
      return null;
    }
    return LocalDateTime.ofInstant(timestamp.toInstant(), ZoneId.of("UTC"));
  }

  default Integer toLabelValue(LabelInfo labelInfo) {
    if (Objects.nonNull(labelInfo.approved)) {
      return 1;
    }
    if (Objects.nonNull(labelInfo.rejected)) {
      return -1;
    }
    return 0;
  }
}
