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

import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.FileInfo;
import com.google.gerrit.extensions.common.LabelInfo;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class GerritMapperTest {

  GerritMapper gerritMapper = Mappers.getMapper(GerritMapper.class);

  @Test
  void toFileDtoTest() {
    var fileInfo = new FileInfo();
    fileInfo.status = 'A';
    fileInfo.size = 36;
    fileInfo.linesDeleted = 0;
    fileInfo.sizeDelta = 12;
    fileInfo.linesInserted = 12;
    fileInfo.oldPath = RandomString.make();
    var key = RandomString.make();
    Map<String, FileInfo> map = new HashMap<>();
    map.put(key, fileInfo);

    var stringFileInfoDtoMap = gerritMapper.toFileDto(map);
    var fileInfoDto = stringFileInfoDtoMap.get(key);
    Assertions.assertThat(fileInfoDto).isNotNull();
    Assertions.assertThat(fileInfoDto.getStatus()).isEqualTo(fileInfo.status.toString());
    Assertions.assertThat(fileInfoDto.getSize()).isEqualTo(fileInfo.size);
    Assertions.assertThat(fileInfoDto.getLinesDeleted()).isEqualTo(fileInfo.linesDeleted);
    Assertions.assertThat(fileInfoDto.getLinesInserted()).isEqualTo(fileInfo.linesInserted);
    Assertions.assertThat(fileInfoDto.getSizeDelta()).isEqualTo(fileInfo.sizeDelta);
  }

  @Test
  void toChangeInfoDtoTest() {
    var number = 10;
    var username = RandomString.make();
    var email = RandomString.make();
    var owner = new AccountInfo(username, email);

    var changeInfo = new ChangeInfo();
    changeInfo._number = number;
    changeInfo.mergeable = true;
    changeInfo.created = Timestamp.valueOf(LocalDateTime.of(2022, 8, 10, 13, 18));
    changeInfo.updated = Timestamp.valueOf(LocalDateTime.of(2022, 8, 10, 13, 28));
    changeInfo.submitted = Timestamp.valueOf(LocalDateTime.of(2022, 8, 10, 13, 18));
    changeInfo.owner = owner;

    var changeInfoDto = gerritMapper.toChangeInfoDto(changeInfo);
    Assertions.assertThat(changeInfoDto).isNotNull();
    Assertions.assertThat(changeInfoDto.getNumber()).isEqualTo(String.valueOf(changeInfo._number));
    Assertions.assertThat(changeInfoDto.getMergeable()).isEqualTo(changeInfo.mergeable);
    Assertions.assertThat(changeInfoDto.getCreated()).isEqualTo(gerritMapper.toLocalDateTime(changeInfo.created));
    Assertions.assertThat(changeInfoDto.getUpdated()).isEqualTo(gerritMapper.toLocalDateTime(changeInfo.updated));
    Assertions.assertThat(changeInfoDto.getSubmitted()).isEqualTo(gerritMapper.toLocalDateTime(changeInfo.submitted));
    Assertions.assertThat(changeInfoDto.getOwner()).isEqualTo(changeInfo.owner.username);
  }

  @Test
  void toLabelMapTestApprovedTrue() {
    var labelInfo = new LabelInfo();
    var key = RandomString.make();
    labelInfo.approved = new AccountInfo(1);
    Map<String, LabelInfo> labelInfoMap = new HashMap<>();
    labelInfoMap.put(key, labelInfo);
    Map<String, Integer> actual = gerritMapper.toLabelMap(labelInfoMap);
    Assertions.assertThat(actual).isNotNull();
    Assertions.assertThat(actual.get(key)).isEqualTo(1);
  }

  @Test
  void toLabelMapTestApprovedUnknown() {
    var labelInfo = new LabelInfo();
    var key = RandomString.make();
    Map<String, LabelInfo> labelInfoMap = new HashMap<>();
    labelInfoMap.put(key, labelInfo);
    Map<String, Integer> actual = gerritMapper.toLabelMap(labelInfoMap);
    Assertions.assertThat(actual).isNotNull();
    Assertions.assertThat(actual.get(key)).isEqualTo(0);
  }

  @Test
  void toLabelMapTestApprovedFalse() {
    var labelInfo = new LabelInfo();
    labelInfo.rejected = new AccountInfo(1);
    var key = RandomString.make();
    Map<String, LabelInfo> labelInfoMap = new HashMap<>();
    labelInfoMap.put(key, labelInfo);
    Map<String, Integer> actual = gerritMapper.toLabelMap(labelInfoMap);
    Assertions.assertThat(actual).isNotNull();
    Assertions.assertThat(actual.get(key)).isEqualTo(-1);
  }

  @Test
  void toLabelMapExpectNullTest() {
    Assertions.assertThat(gerritMapper.toLabelMap(null)).isEqualTo(null);
  }

}
