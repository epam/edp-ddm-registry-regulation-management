/*
 * Copyright 2023 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.management.forms;

import com.epam.digital.data.platform.management.core.utils.ETagUtils;
import com.epam.digital.data.platform.management.filemanagement.model.VersionedFileInfoDto;
import com.epam.digital.data.platform.management.forms.model.FormInfoDto;
import com.epam.digital.data.platform.management.gitintegration.model.FileDatesDto;
import com.jayway.jsonpath.JsonPath;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

/**
 * Map versioned file dto to form dto
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface FormMapper {

  @Mapping(target = "created", source = "datesDto.create", defaultExpression = "java(fileInfoDto.getCreated())")
  @Mapping(target = "updated", source = "datesDto.update", defaultExpression = "java(fileInfoDto.getUpdated())")
  @Mapping(target = "title", source = "formContent", qualifiedByName = "getTitleFromFormContent")
  @Mapping(target = "etag", source = "etag")
  FormInfoDto toForm(VersionedFileInfoDto fileInfoDto, FileDatesDto datesDto, String formContent,
      boolean conflicted, String etag);

  @Named("getTitleFromFormContent")
  default String getTitleFromFormContent(String formContent) {
    return JsonPath.read(formContent, "$.title");
  }
}
