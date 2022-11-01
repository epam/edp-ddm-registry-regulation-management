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

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.config.JacksonConfig;
import com.epam.digital.data.platform.management.exception.BusinessProcessAlreadyExists;
import com.epam.digital.data.platform.management.exception.GerritCommunicationException;
import com.epam.digital.data.platform.management.exception.ProcessNotFoundException;
import com.epam.digital.data.platform.management.exception.ReadingRepositoryException;
import com.epam.digital.data.platform.management.exception.RepositoryNotFoundException;
import com.epam.digital.data.platform.management.model.dto.BusinessProcessResponse;
import com.epam.digital.data.platform.management.model.dto.FileDatesDto;
import com.epam.digital.data.platform.management.model.dto.FileResponse;
import com.epam.digital.data.platform.management.model.dto.FileStatus;
import com.epam.digital.data.platform.management.service.BusinessProcessService;
import com.epam.digital.data.platform.management.service.VersionedFileRepository;
import com.epam.digital.data.platform.management.service.VersionedFileRepositoryFactory;
import com.google.gerrit.extensions.restapi.RestApiException;
import java.net.URISyntaxException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BusinessProcessServiceImpl implements BusinessProcessService {
  private static final String DIRECTORY_PATH = "bpmn";
  private static final String BPMN_FILE_EXTENSION = "bpmn";
  private final VersionedFileRepositoryFactory repoFactory;

  private final GerritPropertiesConfig gerritPropertiesConfig;
  private final DocumentBuilder documentBuilder;
  public static final String PROCESS_CREATED_PATH = "rrm:created";
  public static final String PROCESS_MODIFIED_PATH = "rrm:modified";

  @Override
  public List<BusinessProcessResponse> getProcessesByVersion(String versionName) {
    return getProcessesByVersion(versionName, FileStatus.DELETED);
  }

  @Override
  public List<BusinessProcessResponse> getChangedProcessesByVersion(String versionName) {
    return getProcessesByVersion(versionName, FileStatus.CURRENT);
  }

  @Override
  public void createProcess(String processName, String content, String versionName)
      throws RestApiException, GitAPIException, URISyntaxException, IOException {
    VersionedFileRepository repo;
    String processPath;
    repo = repoFactory.getRepoByVersion(versionName);
    processPath = getProcessPath(processName);
    if (repo.isFileExists(processPath)) {
      throw new BusinessProcessAlreadyExists(String.format("Process with path '%s' already exists", processPath));
    }
    content = addDatesToContent(content, LocalDateTime.now(), LocalDateTime.now());
    repo.writeFile(processPath, content);
  }

  @Override
  public String getProcessContent(String processName, String versionName) {
    VersionedFileRepository repo = repoFactory.getRepoByVersion(versionName);
    String processContent;
    try {
      processContent = repo.readFile(getProcessPath(processName));
    } catch (IOException e) {
      throw new ReadingRepositoryException("Could no read repo to get process content", e);
    }
    if (processContent == null) {
      throw new ProcessNotFoundException("Process " + processName + " not found", processName);
    }
    return processContent;
  }

  @Override
  public void updateProcess(String content, String processName, String versionName) {
    VersionedFileRepository repo = repoFactory.getRepoByVersion(versionName);
    String processPath = getProcessPath(processName);
    var time = LocalDateTime.now();
    FileDatesDto fileDatesDto = FileDatesDto.builder().build();
    try {
      if (repo.isFileExists(processPath)) {
        String oldContent = repo.readFile(processPath);
        fileDatesDto = getDatesFromContent(oldContent);
      }
      if (fileDatesDto.getCreate() == null) {
        fileDatesDto.setCreate(repo.getFileList(DIRECTORY_PATH).stream()
            .filter(fileResponse -> fileResponse.getName().equals(processName))
            .findFirst().map(FileResponse::getCreated).orElse(time));
      }
      content = addDatesToContent(content, fileDatesDto.getCreate(), time);
      repo.writeFile(processPath, content);
    } catch (Exception e) {
      throw new ReadingRepositoryException("Could not read repo to update process", e);
    }
  }

  @Override
  public void deleteProcess(String processName, String versionName) {
    try {
      VersionedFileRepository repo = repoFactory.getRepoByVersion(versionName);
      repo.deleteFile(getProcessPath(processName));
    } catch (RepositoryNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new ReadingRepositoryException("Could not read repo to delete process", e);
    }
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
    VersionedFileRepository masterRepo;
    List<FileResponse> fileList;
    try {
      repo = repoFactory.getRepoByVersion(versionName);
      fileList = repo.getFileList(DIRECTORY_PATH);
      masterRepo = repoFactory.getRepoByVersion(gerritPropertiesConfig.getHeadBranch());
    } catch (RepositoryNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new ReadingRepositoryException("Could not read repo to get process", e);
    }
    List<BusinessProcessResponse> processes = new ArrayList<>();
    for (FileResponse fileResponse : fileList) {
      if (fileResponse.getStatus().equals(skippedStatus)) {
        continue;
      }
      String processContent;
      try {
        if (fileResponse.getStatus() == FileStatus.DELETED) {
          processContent = masterRepo.readFile(getProcessPath(fileResponse.getName()));
        } else {
          processContent = getProcessContent(fileResponse.getName(), versionName);
        }
      } catch (Exception e) {
        throw new GerritCommunicationException("Could not read file from master", e);
      }
      FileDatesDto fileDatesDto = getDatesFromContent(processContent);
      processes.add(BusinessProcessResponse.builder()
          .name(fileResponse.getName())
          .path(fileResponse.getPath())
          .status(fileResponse.getStatus())
          .created(Optional.ofNullable(fileDatesDto.getCreate()).orElse(fileResponse.getCreated()))
          .updated(Optional.ofNullable(fileDatesDto.getUpdate()).orElse(fileResponse.getUpdated()))
          .title(getNameFromContent(processContent))
          .build());
    }
    return processes;
  }

  private FileDatesDto getDatesFromContent(String processContent) {
    FileDatesDto fileDatesDto = FileDatesDto.builder().build();
    try {
      Document doc = documentBuilder.parse(new InputSource(new StringReader(processContent)));
      doc.getDocumentElement().normalize();
      Element element = doc.getDocumentElement();
      if (element.hasAttribute(PROCESS_MODIFIED_PATH)) {
        fileDatesDto.setUpdate(LocalDateTime.parse(element.getAttribute(PROCESS_MODIFIED_PATH), JacksonConfig.DATE_TIME_FORMATTER));
      }
      if (element.hasAttribute(PROCESS_CREATED_PATH)) {
        fileDatesDto.setCreate(LocalDateTime.parse(element.getAttribute(PROCESS_CREATED_PATH), JacksonConfig.DATE_TIME_FORMATTER));
      }
    } catch (SAXException | IOException exception) {
      throw new RuntimeException("Could not parse xml document", exception);
    }
    return fileDatesDto;
  }

  private String addDatesToContent(String processContent, LocalDateTime created, LocalDateTime modified) {
    try {
      Document doc = documentBuilder.parse(new InputSource(new StringReader(processContent)));
      Element element = doc.getDocumentElement();
      element.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:rrm", "http://registry-regulation-management");
      element.setAttributeNS("http://registry-regulation-management", PROCESS_MODIFIED_PATH, modified.format(JacksonConfig.DATE_TIME_FORMATTER));
      element.setAttributeNS("http://registry-regulation-management", PROCESS_CREATED_PATH, created.format(JacksonConfig.DATE_TIME_FORMATTER));
      doc.setXmlStandalone(true);
      return getStringFromDocument(doc);
    } catch (SAXException | IOException exception) {
      throw new RuntimeException("Could not parse xml document", exception);
    }
  }

  private String getStringFromDocument(Document doc) {
    try {
      DOMSource domSource = new DOMSource(doc);
      StringWriter writer = new StringWriter();
      StreamResult result = new StreamResult(writer);
      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer transformer = tf.newTransformer();
      transformer.transform(domSource, result);
      return writer.toString();
    } catch (TransformerException ex) {
      throw new RuntimeException("Could not parse xml document", ex);
    }
  }
}
