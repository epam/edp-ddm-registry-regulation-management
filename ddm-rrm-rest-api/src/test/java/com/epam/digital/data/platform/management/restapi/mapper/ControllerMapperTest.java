/*
 * Copyright 2023 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.management.restapi.mapper;

import com.epam.digital.data.platform.management.restapi.model.ResultValues;
import com.epam.digital.data.platform.management.restapi.model.Validation;
import com.epam.digital.data.platform.management.restapi.model.ValidationType;
import com.epam.digital.data.platform.management.versionmanagement.model.VersionInfoDto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class ControllerMapperTest {

  ControllerMapper mapper = Mappers.getMapper(ControllerMapper.class);

  @Test
  void toVersionInfoDetailedTest() {
    var versionDetails = VersionInfoDto.builder()
        .number(1)
        .subject("JohnDoe's version candidate")
        .description("Version candidate to change form")
        .owner("JohnDoe@epam.com")
        .created(LocalDateTime.of(2022, 8, 10, 11, 30))
        .updated(LocalDateTime.of(2022, 8, 10, 11, 40))
        .mergeable(true)
        .labels(Map.of("Verified", 1))
        .build();
    var expectedValidation = Validation.builder()
        .type(ValidationType.DEPLOYMENT_STATUS)
        .result(ResultValues.SUCCESS)
        .name(ValidationType.DEPLOYMENT_STATUS.getName())
        .build();

    var versionInfoDetailed = mapper.toVersionInfoDetailed(versionDetails);

    Assertions.assertThat(versionInfoDetailed.getId()).isEqualTo(String.valueOf(versionDetails.getNumber()));
    Assertions.assertThat(versionInfoDetailed.getName()).isEqualTo(versionDetails.getSubject());
    Assertions.assertThat(versionInfoDetailed.getAuthor()).isEqualTo(versionDetails.getOwner());
    Assertions.assertThat(versionInfoDetailed.getCreationDate()).isEqualTo(versionDetails.getCreated());
    Assertions.assertThat(versionInfoDetailed.getLatestUpdate()).isEqualTo(versionDetails.getUpdated());
    Assertions.assertThat(versionInfoDetailed.getHasConflicts()).isFalse();
    Assertions.assertThat(versionInfoDetailed.getValidations()).isEqualTo(List.of(expectedValidation));
  }
}
