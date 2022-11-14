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

package com.epam.digital.data.platform.management.filemanagement.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.epam.digital.data.platform.management.filemanagement.mapper.FileManagementMapper;
import com.epam.digital.data.platform.management.filemanagement.model.FileStatus;
import com.epam.digital.data.platform.management.filemanagement.model.VersionedFileInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.model.ChangeInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.model.FileInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.service.GerritService;
import com.epam.digital.data.platform.management.gitintegration.model.FileDatesDto;
import com.epam.digital.data.platform.management.gitintegration.service.JGitService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class VersionedFileRepositoryTest {

  @Mock
  private JGitService jGitService;
  @Mock
  private GerritService gerritService;
  @Spy
  private FileManagementMapper mapper = Mappers.getMapper(FileManagementMapper.class);

  private VersionedFileRepository repository;

  @BeforeEach
  void setUp() {
    repository = new VersionedFileRepositoryImpl("version", jGitService, gerritService, mapper);
  }

  @Test
  @SneakyThrows
  void getFileListTest() {
    FileDatesDto fileDates = FileDatesDto.builder()
        .create(LocalDateTime.now())
        .update(LocalDateTime.now())
        .build();
    List<String> list = new ArrayList<>();
    list.add("file1");
    list.add("file2");
    Mockito.when(jGitService.getFilesInPath(any(), eq("folder"))).thenReturn(list);
    Mockito.when(jGitService.getDates(any(), any())).thenReturn(fileDates);
    List<VersionedFileInfoDto> fileList = repository.getFileList("folder");
    Assertions.assertThat(fileList).isNotNull();
  }

  @Test
  @SneakyThrows
  void getVersionedFileListTest() {
    List<String> list = new ArrayList<>();
    FileDatesDto fileDates = FileDatesDto.builder()
        .create(LocalDateTime.now())
        .update(LocalDateTime.now())
        .build();
    list.add("folder/file1");
    list.add("folder/file2");
    list.add("folder/file3");
    var changeInfo = new ChangeInfoDto();
    changeInfo.setCreated(LocalDateTime.now());
    changeInfo.setUpdated(LocalDateTime.now());
    var filesInMR = new HashMap<String, FileInfoDto>();
    filesInMR.put("folder/file22", new FileInfoDto());
    filesInMR.put("folder/file3", new FileInfoDto());

    Mockito.when(gerritService.getMRByNumber(any())).thenReturn(changeInfo);
    Mockito.when(gerritService.getListOfChangesInMR(any())).thenReturn(filesInMR);
    Mockito.when(jGitService.getFilesInPath(any(), eq("folder"))).thenReturn(list);
    Mockito.when(jGitService.getDates(any(), any())).thenReturn(fileDates);
    List<VersionedFileInfoDto> fileList = repository.getFileList("folder");
    Assertions.assertThat(fileList).isNotNull();
    Assertions.assertThat(fileList.size()).isEqualTo(4);
  }

  @Test
  @SneakyThrows
  void getVersionedFileListWithStatusesTest() {
    FileDatesDto fileDates = FileDatesDto.builder()
        .create(LocalDateTime.now())
        .update(LocalDateTime.now())
        .build();
    var changeInfo = new ChangeInfoDto();
    changeInfo.setCreated(LocalDateTime.now());
    var addedFileInfo = new FileInfoDto();
    addedFileInfo.setStatus("A");
    var deletedFileInfo = new FileInfoDto();
    deletedFileInfo.setStatus("D");
    var renamedFileInfo = new FileInfoDto();
    renamedFileInfo.setStatus("R");
    var copiedFileInfo = new FileInfoDto();
    copiedFileInfo.setStatus("C");

    Mockito.when(gerritService.getMRByNumber(any())).thenReturn(changeInfo);
    Mockito.when(gerritService.getListOfChangesInMR(any())).thenReturn(
        Map.of("folder/file12", addedFileInfo,
            "folder/file2", deletedFileInfo,
            "folder/file14", renamedFileInfo,
            "folder/file3", new FileInfoDto(),
            "folder/file2copy", copiedFileInfo));
    Mockito.when(jGitService.getFilesInPath(any(), eq("folder"))).thenReturn(
        List.of("file1", "file2", "file3", "file2copy"));
    Mockito.when(jGitService.getDates(any(), any())).thenReturn(fileDates);
    List<VersionedFileInfoDto> fileList = repository.getFileList("folder");
    Assertions.assertThat(fileList).isNotNull();
    Assertions.assertThat(fileList.size()).isEqualTo(6);
    Assertions.assertThat(FileStatus.CURRENT).isEqualTo(getFileStatusByName(fileList, "file1"));
    Assertions.assertThat(FileStatus.DELETED).isEqualTo(getFileStatusByName(fileList, "file2"));
    Assertions.assertThat(FileStatus.CHANGED).isEqualTo(getFileStatusByName(fileList, "file3"));
    Assertions.assertThat(FileStatus.NEW).isEqualTo(getFileStatusByName(fileList, "file12"));
    Assertions.assertThat(FileStatus.CHANGED).isEqualTo(getFileStatusByName(fileList, "file14"));
    Assertions.assertThat(FileStatus.NEW).isEqualTo(getFileStatusByName(fileList, "file2copy"));
  }

  private FileStatus getFileStatusByName(List<VersionedFileInfoDto> files, String name) {
    return files.stream()
        .filter(e -> name.equals(e.getName()))
        .findAny()
        .map(VersionedFileInfoDto::getStatus)
        .orElse(null);
  }

  @Test
  @SneakyThrows
  void writeFileTest() {
    final var filepath = "folder/" + RandomString.make();
    final var fileContent = RandomString.make();

    repository.writeFile(filepath, fileContent);

    Mockito.verify(jGitService).amend("version", filepath, fileContent);
  }

  @Test
  @SneakyThrows
  void deleteTest() {
    final var filepath = "folder/" + RandomString.make();

    assertThatCode(() -> repository.deleteFile(filepath))
        .doesNotThrowAnyException();

    Mockito.verify(jGitService).delete("version", filepath);
  }

  @Test
  @SneakyThrows
  void readFileTest() {
    Mockito.when(jGitService.getFileContent(any(), any())).thenReturn("");
    String file = repository.readFile("/");
    Assertions.assertThat(file).isNotNull();
    Mockito.verify(jGitService).getFileContent(any(), any());
  }

  @Test
  @SneakyThrows
  void pullRepositoryTest() {
    var changeInfo = new ChangeInfoDto();
    changeInfo.setChangeId("1");
    final var mock = Mockito.mock(ChangeInfoDto.class);
    Mockito.when(gerritService.getChangeInfo(changeInfo.getChangeId())).thenReturn(mock);
    Mockito.when(gerritService.getMRByNumber("version")).thenReturn(changeInfo);
    repository.updateRepository();
    Mockito.verify(jGitService, Mockito.times(1)).cloneRepoIfNotExist("version");
  }

  @Test
  @SneakyThrows
  void isFileExistsTest() {
    FileDatesDto fileDates = FileDatesDto.builder()
        .create(LocalDateTime.now())
        .update(LocalDateTime.now())
        .build();
    ArrayList<String> t = new ArrayList<>();
    t.add("fileName");
    Mockito.when(jGitService.getFilesInPath(any(), any())).thenReturn(t);
    Mockito.when(jGitService.getDates(any(), any())).thenReturn(fileDates);
    boolean fileExists = repository.isFileExists("folder/fileName");
    Assertions.assertThat(fileExists).isTrue();
  }
}
