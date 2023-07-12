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

import com.epam.digital.data.platform.management.filemanagement.mapper.FileManagementMapper;
import com.epam.digital.data.platform.management.filemanagement.model.VersionedFileInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.service.GerritService;
import com.epam.digital.data.platform.management.gitintegration.model.FileDatesDto;
import com.epam.digital.data.platform.management.gitintegration.service.JGitService;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class HeadFileRepositoryTest {

  @Mock
  private JGitService jGitService;
  @Mock
  private GerritService gerritService;
  @Spy
  private FileManagementMapper mapper = Mappers.getMapper(FileManagementMapper.class);

  private VersionedFileRepository repository;

  @BeforeEach
  void setUp() {
    repository = new HeadFileRepositoryImpl("version", jGitService, gerritService, mapper);
  }

  @Test
  @SneakyThrows
  void getFileListTest() {
    var path = RandomString.make();
    var normalizePath = FilenameUtils.normalize(Path.of(path, path).toString(), true);
    var fileDatesDto = FileDatesDto.builder().create(LocalDateTime.now())
        .update(LocalDateTime.now()).build();
    List<String> list = new ArrayList<>();
    list.add(path);

    Mockito.when(jGitService.getFilesInPath("version", path)).thenReturn(list);
    Mockito.when(jGitService.getDates("version", normalizePath)).thenReturn(fileDatesDto);

    List<VersionedFileInfoDto> fileList = repository.getFileList(path);
    Assertions.assertThat(fileList).isNotNull();
    var versionedFileInfoDto = fileList.stream()
        .filter(file -> path.equals(file.getName())).findFirst().orElse(null);

    Assertions.assertThat(versionedFileInfoDto).isNotNull();
    Assertions.assertThat(versionedFileInfoDto.getName()).isEqualTo(path);

    Mockito.verify(jGitService).getFilesInPath("version", path);
    Mockito.verify(jGitService).getDates("version", normalizePath);
    Mockito.verify(mapper).toVersionedFileInfoDto(normalizePath, fileDatesDto);
  }

  @Test
  @SneakyThrows
  void getFileListDatesNullTest() {
    var path = RandomString.make();
    var normalizePath = FilenameUtils.normalize(Path.of(path, path).toString(), true);
    List<String> list = new ArrayList<>();
    list.add(path);

    Mockito.when(jGitService.getFilesInPath("version", path)).thenReturn(list);
    Mockito.when(jGitService.getDates("version", normalizePath)).thenReturn(null);
    Assertions.assertThatCode(() -> repository.getFileList(path))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Got null dates on existing file in the git repo");

    Mockito.verify(jGitService).getFilesInPath("version", path);
    Mockito.verify(jGitService).getDates("version", normalizePath);
    Mockito.verify(mapper, Mockito.never()).toVersionedFileInfoDto(normalizePath, null);
  }

  @Test
  void deleteNotSupportTest() {
    Assertions.assertThatCode(() -> repository.deleteFile("/"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @SneakyThrows
  void readFileTest() {
    var path = RandomString.make();
    var content = RandomString.make();
    Mockito.when(jGitService.getFileContent("version", path)).thenReturn(content);
    var fileContent = repository.readFile(path);
    Assertions.assertThat(fileContent).isNotNull();
    Assertions.assertThat(fileContent).isEqualTo(content);
  }

  @Test
  @SneakyThrows
  void pullRepositoryTest() {
    repository.updateRepository();
    Mockito.verify(jGitService, Mockito.times(1))
        .cloneRepoIfNotExist("version");
  }

  @Test
  @SneakyThrows
  void isFileExistsTest() {
    ArrayList<String> files = new ArrayList<>();
    files.add("fileName");

    final String normalize = FilenameUtils.normalize("/");
    Mockito.when(jGitService.getFilesInPath("version", normalize)).thenReturn(files);
    var fileExists = repository.isFileExists("/fileName");

    Assertions.assertThat(fileExists).isTrue();
    Mockito.verify(jGitService).getFilesInPath("version", normalize);
  }
}
