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

package com.epam.digital.data.platform.management.service.impl;

import com.epam.digital.data.platform.management.exception.BusinessProcessAlreadyExists;
import com.epam.digital.data.platform.management.exception.ReadingRepositoryException;
import com.epam.digital.data.platform.management.model.dto.BusinessProcessResponse;
import com.epam.digital.data.platform.management.model.dto.FileResponse;
import com.epam.digital.data.platform.management.model.dto.FileStatus;
import com.epam.digital.data.platform.management.service.BusinessProcessService;
import com.epam.digital.data.platform.management.service.VersionedFileRepository;
import com.epam.digital.data.platform.management.service.VersionedFileRepositoryFactory;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BusinessProcessServiceImpl implements BusinessProcessService {
  private static final String DIRECTORY_PATH = "bpmn";
  private static final String BPMN_FILE_EXTENSION = "bpmn";

  private final VersionedFileRepositoryFactory repoFactory;

  private final DocumentBuilder documentBuilder;

  @Override
  public List<BusinessProcessResponse> getProcessesByVersion(String versionName) {
    return getProcessesByVersion(versionName, FileStatus.DELETED);
  }

  @Override
  public List<BusinessProcessResponse> getChangedProcessesByVersion(String versionName) {
    return getProcessesByVersion(versionName, FileStatus.CURRENT);
  }

  @Override
  public void createProcess(String processName, String content, String versionName) throws Exception {
    VersionedFileRepository repo;
    String processPath;
    try {
      repo = repoFactory.getRepoByVersion(versionName);
      processPath = getProcessPath(processName);
    } catch (Exception e) {
      throw new ReadingRepositoryException("Could not read repo to create process", e);
    }
    if (repo.isFileExists(processPath)) {
      throw new BusinessProcessAlreadyExists(String.format("Process with path '%s' already exists", processPath));
    }
    repo.writeFile(processPath, content);
  }

  @Override
  public String getProcessContent(String processName, String versionName) {
    try {
      VersionedFileRepository repo = repoFactory.getRepoByVersion(versionName);
      return repo.readFile(
          getProcessPath(processName));
    } catch (Exception e) {
      throw new ReadingRepositoryException("Could no read repo to get process content", e);
    }
  }

  @Override
  public void updateProcess(String content, String processName, String versionName) {
    try {
      VersionedFileRepository repo = repoFactory.getRepoByVersion(versionName);
      repo.writeFile(getProcessPath(processName), content);
    } catch (Exception e) {
      throw new ReadingRepositoryException("Could not read repo to update process", e);
    }
  }

  @Override
  public void deleteProcess(String processName, String versionName) {
    try {
      VersionedFileRepository repo = repoFactory.getRepoByVersion(versionName);
      repo.deleteFile(getProcessPath(processName));
    } catch (Exception e) {
      throw new ReadingRepositoryException("Could not read repo to delete process", e);
    }
  }

  private String getProcessContent(VersionedFileRepository repo, FileResponse fileResponse) {
    String processContent;
    try {
      processContent = repo.readFile(
          getProcessPath(fileResponse.getName()));
    } catch (Exception e) {
      throw new ReadingRepositoryException("Could not read file from repo", e);
    }
    return processContent;
  }

  private String getNameFromContent(String processContent) {
    try {
      Document doc = documentBuilder.parse(new InputSource(new StringReader(processContent)));
      doc.getDocumentElement().normalize();
      NodeList nodeList = doc.getElementsByTagName("bpmn:process");
      Node node = nodeList.item(0);
      return node.getAttributes().getNamedItem("name").getTextContent();
    } catch (SAXException | IOException exception) {
      throw new RuntimeException("Could not parse xml document", exception);
    }
  }

  private String getProcessPath(String processName) {
    return String.format("%s/%s.%s", DIRECTORY_PATH, FilenameUtils.getName(processName),
        BPMN_FILE_EXTENSION);
  }

  private List<BusinessProcessResponse> getProcessesByVersion(String versionName, FileStatus skippedStatus) {
    VersionedFileRepository repo;
    List<FileResponse> fileList;
    try {
      repo = repoFactory.getRepoByVersion(versionName);
      fileList = repo.getFileList(DIRECTORY_PATH);
    } catch (Exception e) {
      throw new ReadingRepositoryException("Could not read repo to get process", e);
    }
    List<BusinessProcessResponse> processes = new ArrayList<>();
    for (FileResponse fileResponse : fileList) {
      if (fileResponse.getStatus().equals(skippedStatus)) {
        continue;
      }
      String processContent;
      processContent = getProcessContent(repo, fileResponse);
      processes.add(BusinessProcessResponse.builder()
          .name(fileResponse.getName())
          .path(fileResponse.getPath())
          .status(fileResponse.getStatus())
          .created(fileResponse.getCreated())
          .updated(fileResponse.getUpdated())
          .title(getNameFromContent(processContent))
          .build());
    }
    return processes;
  }
}
