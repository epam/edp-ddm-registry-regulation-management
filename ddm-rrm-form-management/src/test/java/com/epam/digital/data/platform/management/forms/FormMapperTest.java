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
import com.epam.digital.data.platform.management.filemanagement.model.FileStatus;
import com.epam.digital.data.platform.management.filemanagement.model.VersionedFileInfoDto;
import com.epam.digital.data.platform.management.forms.model.FormInfoDto;
import com.epam.digital.data.platform.management.forms.util.TestUtils;
import com.epam.digital.data.platform.management.gitintegration.model.FileDatesDto;
import java.time.LocalDateTime;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class FormMapperTest {

  private final String FORM_CONTENT = TestUtils.getContent("form-sample.json");
  private final FormMapper mapper = Mappers.getMapper(FormMapper.class);

  @Test
  @SneakyThrows
  void mapToFormInfoDtoTest() {
    var fileInfo = VersionedFileInfoDto.builder()
        .name("form")
        .path("forms/form.json")
        .status(FileStatus.NEW)
        .created(LocalDateTime.of(2022, 8, 10, 13, 18))
        .updated(LocalDateTime.of(2022, 8, 10, 13, 28))
        .build();
    var fileDates = FileDatesDto.builder()
        .create(LocalDateTime.of(2022, 8, 10, 13, 18))
        .update(LocalDateTime.of(2022, 8, 10, 13, 28))
        .build();
    var expected = FormInfoDto.builder()
        .title("Update physical factors")
        .name("form")
        .path("forms/form.json")
        .status(FileStatus.NEW)
        .created(LocalDateTime.of(2022, 8, 10, 13, 18))
        .updated(LocalDateTime.of(2022, 8, 10, 13, 28))
        .conflicted(true)
        .etag(ETagUtils.getETagFromContent(FORM_CONTENT))
        .build();
    var actual = mapper.toForm(fileInfo, fileDates, FORM_CONTENT, true,
        ETagUtils.getETagFromContent(FORM_CONTENT));
    Assertions.assertThat(actual).isEqualTo(expected);
  }
}