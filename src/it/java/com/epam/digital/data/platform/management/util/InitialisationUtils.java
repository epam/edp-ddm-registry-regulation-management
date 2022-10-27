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

package com.epam.digital.data.platform.management.util;

import com.epam.digital.data.platform.management.BaseIT;
import com.epam.digital.data.platform.management.dto.TestFormDetailsShort;
import com.epam.digital.data.platform.management.model.dto.BusinessProcessDetailsShort;
import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.RevisionInfo;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
import org.assertj.core.internal.bytebuddy.utility.RandomString;

public class InitialisationUtils extends BaseIT {

  @SneakyThrows
  public static ChangeInfoDto initChangeInfoDto(String refs) {
    ChangeInfoDto changeInfoDto = new ChangeInfoDto();
    changeInfoDto.setRefs(refs);
    return changeInfoDto;
  }

  @SneakyThrows
  public static ChangeInfoDto initChangeInfoDto(ChangeInfo changeInfo) {
    final var changeInfoDto = new ChangeInfoDto();
    changeInfoDto.setRefs(changeInfo.revisions.get(changeInfo.currentRevision).ref);
    changeInfoDto.setNumber(String.valueOf(changeInfo._number));
    changeInfoDto.setChangeId(changeInfo.changeId);
    changeInfoDto.setSubject(changeInfo.subject);
    return changeInfoDto;
  }

  @SneakyThrows
  public static void createTempRepo(String versionName) {
    if (Files.notExists(Paths.get(tempRepoDirectory.getPath(), versionName))) {
      Files.createDirectory(Paths.get(tempRepoDirectory.getPath(), versionName));
      Files.createDirectory(Paths.get(tempRepoDirectory.getPath(), versionName, "forms"));
      Files.createDirectory(Paths.get(tempRepoDirectory.getPath(), versionName, "bpmn"));
    }
  }

  @SneakyThrows
  public static void deleteFormJson(String path) {
    Files.delete(Paths.get(path));
  }

  @SneakyThrows
  public static String createFormJson(TestFormDetailsShort form, String versionName) {
    String filePath =
        tempRepoDirectory.getPath() + "/" + versionName + "/forms/" + form.getName() + ".json";
    Files.createFile(Paths.get(filePath));
    FileWriter fileWriter = new FileWriter(filePath);
    fileWriter.write(form.getContent());
    fileWriter.close();
    return filePath;
  }

  @SneakyThrows
  public static String createProcessXml(String content, String versionName, String processName) {
    String filePath =
        tempRepoDirectory.getPath() + "/" + versionName + "/bpmn/" + processName + ".bpmn";
    Files.createFile(Paths.get(filePath));
    FileWriter fileWriter = new FileWriter(filePath);
    fileWriter.write(content);
    fileWriter.close();
    return filePath;
  }

  public static ChangeInfo initChangeInfo(int number) {
    return initChangeInfo(number, "admin", "admin@epam.com", "admin");
  }

  public static ChangeInfo initChangeInfo(int number, String ownerName, String ownerEmail,
      String ownerUsername) {
    ChangeInfo changeInfo = new ChangeInfo();
    changeInfo.id = "id" + number;
    changeInfo.changeId = "id" + number;
    changeInfo._number = number;
    changeInfo.owner = new AccountInfo(ownerName, ownerEmail);
    changeInfo.owner.username = ownerUsername;
    changeInfo.topic = "this is description for version candidate " + number;
    changeInfo.subject = RandomString.make();
    changeInfo.updated = Timestamp.from(
        LocalDateTime.of(2022, 8, 2, 16, 15).toInstant(ZoneOffset.UTC));
    changeInfo.labels = Map.of();
    changeInfo.mergeable = true;

    changeInfo.revisions = new HashMap<>();
    final var revisionInfo = new RevisionInfo();
    revisionInfo.ref = RandomString.make();
    changeInfo.currentRevision = RandomString.make();
    changeInfo.revisions.put(changeInfo.currentRevision, revisionInfo);

    return changeInfo;
  }

  public static TestFormDetailsShort initFormDetails(String name, String title, String content) {
    return TestFormDetailsShort.builder()
        .name(name).title(title)
        .created(LocalDateTime.now())
        .updated(LocalDateTime.now())
        .content(content)
        .build();
  }

  public static BusinessProcessDetailsShort initBusinessProcessDetails(String name, String title) {
    return BusinessProcessDetailsShort.builder()
        .name(name)
        .title(title)
        .created(LocalDateTime.now())
        .updated(LocalDateTime.now())
        .build();
  }
}
