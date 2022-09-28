package com.epam.digital.data.platform.management.util;

import com.epam.digital.data.platform.management.BaseIT;
import com.epam.digital.data.platform.management.model.dto.BusinessProcessDetailsShort;
import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.epam.digital.data.platform.management.model.dto.FormDetailsShort;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gson.Gson;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import lombok.SneakyThrows;

public class InitialisationUtils extends BaseIT {
  private static final Gson gson = new Gson();

  @SneakyThrows
  public static ChangeInfoDto initChangeInfoDto(String refs) {
    ChangeInfoDto changeInfoDto = new ChangeInfoDto();
    changeInfoDto.setRefs(refs);
    return changeInfoDto;
  }

  @SneakyThrows
  public static void createTempRepo(String versionName) {
    if (Files.notExists(Paths.get(tempRepoDirectory.getPath(), versionName))) {
      Files.createDirectory(Paths.get(tempRepoDirectory.getPath(), versionName));
    }
  }

  @SneakyThrows
  public static void deleteFormJson(String path) {
    Files.delete(Paths.get(path));
  }

  @SneakyThrows
  public static String createFormJson(FormDetailsShort form, String versionName) {
    String filePath = tempRepoDirectory.getPath() + "/" + versionName + "/forms/" + form.getName() + ".json";
    Files.createDirectory(Paths.get(tempRepoDirectory.getPath(), versionName, "forms"));
    Files.createFile(Paths.get(filePath));
    FileWriter fileWriter = new FileWriter(filePath);
    fileWriter.write(gson.toJson(form));
    fileWriter.close();
    return filePath;
  }

  @SneakyThrows
  public static String createProcessXml(String content, String versionName, String processName) {
    String filePath = tempRepoDirectory.getPath() + "/" + versionName + "/bpmn/" + processName + ".bpmn";
    Files.createDirectory(Paths.get(tempRepoDirectory.getPath(), versionName, "bpmn"));
    Files.createFile(Paths.get(filePath));
    FileWriter fileWriter = new FileWriter(filePath);
    fileWriter.write(content);
    fileWriter.close();
    return filePath;
  }

  public static ChangeInfo initChangeInfo(int number, String ownerName, String ownerEmail, String ownerUsername) {
    ChangeInfo changeInfo = new ChangeInfo();
    changeInfo.id = "id" + number;
    changeInfo.changeId = "id" + number;
    changeInfo._number = number;
    changeInfo.owner = new AccountInfo(ownerName, ownerEmail);
    changeInfo.owner.username = ownerUsername;
    changeInfo.topic = "this is description for version candidate " + number;
    changeInfo.subject = "commit message";
    changeInfo.updated = Timestamp.from(
        LocalDateTime.of(2022, 8, 2, 16, 15).toInstant(ZoneOffset.UTC));
    changeInfo.labels = Map.of();
    changeInfo.mergeable = true;
    return changeInfo;
  }

  public static FormDetailsShort initFormDetails(String name, String title) {
    return FormDetailsShort.builder()
        .name(name).title(title)
        .created(LocalDateTime.now())
        .updated(LocalDateTime.now())
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
