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

import com.epam.digital.data.platform.management.model.dto.FileResponse;
import com.epam.digital.data.platform.management.service.impl.HeadFileRepositoryImpl;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class HeadFileRepositoryTest {

  @Mock
  private JGitService jGitService;
  @InjectMocks
  private HeadFileRepositoryImpl repository;

  @Test
  @SneakyThrows
  void getFileListTest() {
    List<String> list = new ArrayList<>();
    Mockito.when(jGitService.getFilesInPath(any(), any())).thenReturn(list);
    List<FileResponse> fileList = repository.getFileList("/");
    Assertions.assertNotNull(fileList);
  }

  @Test
  void writeNotSupportTest() {
    Assertions.assertThrows(UnsupportedOperationException.class,
        () -> repository.writeFile("/", "content"));
  }

  @Test
  void deleteNotSupportTest() {
    Assertions.assertThrows(UnsupportedOperationException.class,
        () -> repository.deleteFile("/"));
  }

  @Test
  @SneakyThrows
  void readFileTest() {
    Mockito.when(jGitService.getFileContent(any(), any())).thenReturn("");
    String fileContent = repository.readFile("/");
    Assertions.assertNotNull(fileContent);
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
    ArrayList<String> t = new ArrayList<>();
    t.add("fileName");
    Mockito.when(jGitService.getFilesInPath(any(), any())).thenReturn(t);
    boolean fileExists = repository.isFileExists("/fileName");
    Assertions.assertTrue(fileExists);
  }
}
