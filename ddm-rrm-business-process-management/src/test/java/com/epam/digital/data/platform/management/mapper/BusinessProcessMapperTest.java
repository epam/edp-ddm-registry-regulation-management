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

package com.epam.digital.data.platform.management.mapper;


import com.epam.digital.data.platform.management.TestUtils;
import com.epam.digital.data.platform.management.filemanagement.model.FileStatus;
import com.epam.digital.data.platform.management.filemanagement.model.VersionedFileInfoDto;
import com.epam.digital.data.platform.management.gitintegration.model.FileDatesDto;
import com.epam.digital.data.platform.management.model.dto.BusinessProcessInfoDto;
import java.time.LocalDateTime;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class BusinessProcessMapperTest {
  private final BusinessProcessMapper mapper = Mappers.getMapper(BusinessProcessMapper.class);
  private final String BUSINESS_PROCESS_CONTENT = TestUtils.getContent("bp-sample.bpmn");

  @Test
  @SneakyThrows
  void mapToBusinessProcessInfoDtoTest() {
    var fileInfo = VersionedFileInfoDto.builder()
        .name("business-process")
        .path("bpmn/business-process.bpmn")
        .status(FileStatus.NEW)
        .created(LocalDateTime.of(2022, 8, 10, 13, 18))
        .updated(LocalDateTime.of(2022, 8, 10, 13, 28))
        .build();
    var fileDates = FileDatesDto.builder()
        .create(LocalDateTime.of(2022, 8, 10, 13, 18))
        .update(LocalDateTime.of(2022, 8, 10, 13, 28))
        .build();
    var expected = BusinessProcessInfoDto.builder()
        .title("Really test name")
        .created(LocalDateTime.of(2022, 8, 10, 13, 18))
        .updated(LocalDateTime.of(2022, 8, 10, 13, 28))
        .name("business-process")
        .path("bpmn/business-process.bpmn")
        .status(FileStatus.NEW)
        .build();
    var actual = mapper.toBusinessProcess(fileInfo, fileDates, "Really test name");
    Assertions.assertThat(actual).isEqualTo(expected);
  }
}