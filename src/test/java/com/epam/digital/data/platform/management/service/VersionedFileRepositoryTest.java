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

package com.epam.digital.data.platform.management.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.epam.digital.data.platform.management.model.dto.FileDatesDto;
import com.epam.digital.data.platform.management.model.dto.FileResponse;
import com.epam.digital.data.platform.management.model.dto.FileStatus;
import com.epam.digital.data.platform.management.service.impl.VersionedFileRepositoryImpl;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.FileInfo;
import java.io.File;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class VersionedFileRepositoryTest {

  @Mock
  private JGitService jGitService;
  @Mock
  private GerritService gerritService;
  @InjectMocks
  private VersionedFileRepositoryImpl repository;

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
    Mockito.when(jGitService.getFilesInPath(any(), eq(File.separator))).thenReturn(list);
    Mockito.when(jGitService.getDates(any(), any())).thenReturn(fileDates);
    List<FileResponse> fileList = repository.getFileList(File.separator);
    Assertions.assertNotNull(fileList);
  }

  @Test
  @SneakyThrows
  void getRootFileListTest() {
    FileDatesDto fileDates = FileDatesDto.builder()
        .create(LocalDateTime.now())
        .update(LocalDateTime.now())
        .build();
    List<String> list = new ArrayList<>();
    list.add("file1");
    list.add("file2");
    Mockito.when(jGitService.getFilesInPath(any(), eq(File.separator))).thenReturn(list);
    Mockito.when(jGitService.getDates(any(), any())).thenReturn(fileDates);
    List<FileResponse> fileList = repository.getFileList();
    Assertions.assertNotNull(fileList);
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
    var changeInfo = new ChangeInfo();
    changeInfo.created = new Timestamp(System.currentTimeMillis());
    changeInfo.updated = new Timestamp(System.currentTimeMillis());
    var filesInMR = new HashMap<String, FileInfo>();
    filesInMR.put("folder/file22", new FileInfo());
    filesInMR.put("folder/file3", new FileInfo());

    Mockito.when(gerritService.getMRByNumber(any())).thenReturn(changeInfo);
    Mockito.when(gerritService.getListOfChangesInMR(any())).thenReturn(filesInMR);
    Mockito.when(jGitService.getFilesInPath(any(), eq("folder"))).thenReturn(list);
    Mockito.when(jGitService.getDates(any(), any())).thenReturn(fileDates);
    List<FileResponse> fileList = repository.getFileList("folder");
    Assertions.assertNotNull(fileList);
    Assertions.assertEquals(4, fileList.size());
  }

  @Test
  @SneakyThrows
  void getVersionedFileListWithStatusesTest() {
    FileDatesDto fileDates = FileDatesDto.builder()
        .create(LocalDateTime.now())
        .update(LocalDateTime.now())
        .build();
    var changeInfo = new ChangeInfo();
    changeInfo.created = new Timestamp(System.currentTimeMillis());
    var addedFileInfo = new FileInfo();
    addedFileInfo.status = 'A';
    var deletedFileInfo = new FileInfo();
    deletedFileInfo.status = 'D';
    var renamedFileInfo = new FileInfo();
    renamedFileInfo.status = 'R';

    Mockito.when(gerritService.getMRByNumber(any())).thenReturn(changeInfo);
    Mockito.when(gerritService.getListOfChangesInMR(any())).thenReturn(
        Map.of("folder/file12", addedFileInfo,
            "folder/file2", deletedFileInfo,
            "folder/file14", renamedFileInfo,
            "folder/file3", new FileInfo()));
    Mockito.when(jGitService.getFilesInPath(any(), eq("folder"))).thenReturn(
        List.of("file1", "file2", "file3"));
    Mockito.when(jGitService.getDates(any(), any())).thenReturn(fileDates);
    List<FileResponse> fileList = repository.getFileList("folder");
    Assertions.assertNotNull(fileList);
    Assertions.assertEquals(5, fileList.size());
    Assertions.assertEquals(FileStatus.CURRENT, getFileStatusByName(fileList, "file1"));
    Assertions.assertEquals(FileStatus.DELETED, getFileStatusByName(fileList, "file2"));
    Assertions.assertEquals(FileStatus.CHANGED, getFileStatusByName(fileList, "file3"));
    Assertions.assertEquals(FileStatus.NEW, getFileStatusByName(fileList, "file12"));
    Assertions.assertEquals(FileStatus.CHANGED, getFileStatusByName(fileList, "file14"));
  }

  private FileStatus getFileStatusByName(List<FileResponse> files, String name) {
    return files.stream()
        .filter(e -> name.equals(e.getName()))
        .findAny()
        .map(FileResponse::getStatus)
        .orElse(null);
  }

  @Test
  @SneakyThrows
  void writeFileTest() {
    ChangeInfoDto changeInfoDto = new ChangeInfoDto();
    changeInfoDto.setSubject("change");
    repository.setVersionName("1");
    ChangeInfo changeInfo = new ChangeInfo();
    changeInfo.changeId = "changeId";
    changeInfo._number = 1;
    Mockito.when(gerritService.getChangeInfo("changeId")).thenReturn(changeInfoDto);
    Mockito.when(jGitService.amend(any(), any())).thenReturn("");
    Mockito.when(gerritService.getMRByNumber(eq("1"))).thenReturn(changeInfo);
    repository.writeFile("/form", "content");
    Mockito.verify(gerritService, Mockito.times(1)).getChangeInfo("changeId");
    Mockito.verify(jGitService, Mockito.times(1)).amend(any(), any());
  }

  @Test
  @SneakyThrows
  void deleteTest() {
    FileDatesDto fileDates = FileDatesDto.builder()
        .create(LocalDateTime.now())
        .update(LocalDateTime.now())
        .build();
    repository.setVersionName("1");
    String deleted = repository.deleteFile("form");
    Assertions.assertEquals("File does not exist", deleted);
    ChangeInfo changeInfo = new ChangeInfo();
    changeInfo.changeId = "changeId";
    changeInfo._number = 1;
    ChangeInfoDto changeInfoDto = new ChangeInfoDto();
    changeInfoDto.setSubject("change");
    Mockito.when(gerritService.getMRByNumber(eq("1"))).thenReturn(changeInfo);
    Mockito.when(gerritService.getChangeInfo("changeId")).thenReturn(changeInfoDto);
    Mockito.when(jGitService.getFilesInPath("1", "forms")).thenReturn(List.of("form"));
    Mockito.when(jGitService.delete(any(), any())).thenReturn("deleted");
    Mockito.when(jGitService.getDates(any(), any())).thenReturn(fileDates);
    String form = repository.deleteFile("forms/form.json");
    Assertions.assertEquals("deleted", form);
  }

  @Test
  @SneakyThrows
  void readFileTest() {
    Mockito.when(jGitService.getFileContent(any(), any())).thenReturn("");
    String file = repository.readFile("/");
    Assertions.assertNotNull(file);
    Mockito.verify(jGitService).getFileContent(any(), any());
  }

  @Test
  @SneakyThrows
  void pullRepositoryTest() {
    repository.setVersionName("version");
    repository.pullRepository();
    Mockito.verify(jGitService, Mockito.times(1)).cloneRepo("version");
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
    boolean fileExists = repository.isFileExists("/fileName");
    Assertions.assertTrue(fileExists);
  }
}
