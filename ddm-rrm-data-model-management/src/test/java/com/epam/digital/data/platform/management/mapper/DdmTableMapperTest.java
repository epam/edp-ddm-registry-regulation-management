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


import com.epam.digital.data.platform.management.model.dto.TableInfoDto;
import data.model.snapshot.model.DdmTable;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class DdmTableMapperTest {

  DdmTableMapper mapper = Mappers.getMapper(DdmTableMapper.class);

  @Test
  @SneakyThrows
  void mapToDdmTableTest() {
    var description = RandomString.make();
    var name = RandomString.make();

    final var tableInfoDto = new TableInfoDto();
    tableInfoDto.setDescription(description);
    tableInfoDto.setName(name);
    tableInfoDto.setHistoryFlag(false);
    tableInfoDto.setObjectReference(true);

    final var expectedDdmTable = new DdmTable();
    expectedDdmTable.setDescription(description);
    expectedDdmTable.setName(name);
    expectedDdmTable.setHistoryFlag(false);
    expectedDdmTable.setObjectReference(true);

    Assertions.assertThat(expectedDdmTable).isEqualTo(mapper.convertToDdmTable(tableInfoDto));
  }
}