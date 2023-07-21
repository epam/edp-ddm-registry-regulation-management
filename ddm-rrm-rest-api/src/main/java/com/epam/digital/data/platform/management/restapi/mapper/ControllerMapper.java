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

package com.epam.digital.data.platform.management.restapi.mapper;

import static com.epam.digital.data.platform.management.restapi.model.ResultValues.FAILED;
import static com.epam.digital.data.platform.management.restapi.model.ResultValues.SUCCESS;
import static com.epam.digital.data.platform.management.restapi.model.ResultValues.UNKNOWN;

import com.epam.digital.data.platform.management.gerritintegration.model.CreateChangeInputDto;
import com.epam.digital.data.platform.management.model.dto.TableInfoDto;
import com.epam.digital.data.platform.management.model.dto.TableShortInfoDto;
import com.epam.digital.data.platform.management.restapi.model.CreateVersionRequest;
import com.epam.digital.data.platform.management.restapi.model.ResultValues;
import com.epam.digital.data.platform.management.restapi.model.TableInfo;
import com.epam.digital.data.platform.management.restapi.model.TableInfoShort;
import com.epam.digital.data.platform.management.restapi.model.Validation;
import com.epam.digital.data.platform.management.restapi.model.ValidationType;
import com.epam.digital.data.platform.management.restapi.model.VersionChangesInfo;
import com.epam.digital.data.platform.management.restapi.model.VersionInfoDetailed;
import com.epam.digital.data.platform.management.versionmanagement.model.VersionChangesDto;
import com.epam.digital.data.platform.management.versionmanagement.model.VersionInfoDto;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface ControllerMapper {

  String VERIFIED_LABEL = "Verified";
  Map<Integer, ResultValues> STATUSES = Map.of(
      -1, FAILED,
      0, UNKNOWN,
      1, SUCCESS
  );

  CreateChangeInputDto toDto(CreateVersionRequest request);

  @Mapping(target = "id", source = "number")
  @Mapping(target = "author", source = "owner")
  @Mapping(target = "creationDate", source = "created")
  @Mapping(target = "hasConflicts", source = "mergeable", qualifiedByName = "toConflicts")
  @Mapping(target = "latestUpdate", source = "updated")
  @Mapping(target = "latestRebase", source = "rebased")
  @Mapping(target = "name", source = "subject")
  @Mapping(target = "validations", source = "labels", qualifiedByName = "toValidations")
  VersionInfoDetailed toVersionInfoDetailed(VersionInfoDto versionInfoDto);

  @Named("toConflicts")
  default Boolean toConflicts(Boolean mergeable) {
    return Boolean.FALSE.equals(mergeable);
  }

  @Named("toValidations")
  default List<Validation> toValidations(Map<String, Integer> labels) {
    return List.of(Validation.builder()
        .name(ValidationType.DEPLOYMENT_STATUS.getName())
        .type(ValidationType.DEPLOYMENT_STATUS)
        .result(STATUSES.get(labels.get(VERIFIED_LABEL)))
        .build());
  }

  TableInfo toTableInfo(TableInfoDto tableInfoDto);

  List<TableInfoShort> toTableInfosShort(List<TableShortInfoDto> tableShortInfoDto);

  VersionChangesInfo toVersionChangesInfo(VersionChangesDto dto);
}
