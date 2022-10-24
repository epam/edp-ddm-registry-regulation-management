/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.digital.data.platform.management.service.impl;

import com.epam.digital.data.platform.management.model.dto.FileDatesDto;
import com.epam.digital.data.platform.management.model.dto.FileResponse;
import com.epam.digital.data.platform.management.model.dto.FileStatus;
import com.epam.digital.data.platform.management.service.JGitService;
import com.epam.digital.data.platform.management.service.VersionedFileRepository;
import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.naming.OperationNotSupportedException;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;

@Setter
public class HeadFileRepositoryImpl implements VersionedFileRepository {

  private String versionName;
  private JGitService jGitService;

  @Override
  public List<FileResponse> getFileList() throws Exception {
    return getFileList(File.pathSeparator);
  }

  @Override
  public List<FileResponse> getFileList(String path) throws Exception {
    Map<String, FileResponse> formsInMaster = new HashMap<>();
    List<String> filesInPath = jGitService.getFilesInPath(versionName, path);
    for (String el : filesInPath) {
      if (!el.equals(".gitkeep")) {
        FileDatesDto dates = jGitService.getDates(versionName, path + "/" + el);
        FileResponse build = FileResponse.builder()
            .name(FilenameUtils.getBaseName(el))
            .status(FileStatus.CURRENT)
            .path(path)
            .updated(dates.getUpdate())
            .created(dates.getCreate())
            .build();
        formsInMaster.put(el, build);
      }
    }
    var forms = new ArrayList<>(formsInMaster.values());
    forms.sort(Comparator.comparing(FileResponse::getName));
    return forms;
  }

  @Override
  public void writeFile(String path, String content) throws Exception {
    throw new OperationNotSupportedException();
  }

  @Override
  public String readFile(String path) throws Exception {
    return jGitService.getFileContent(versionName, URLDecoder.decode(path, Charset.defaultCharset()));
  }

  @Override
  public boolean isFileExists(String path) throws Exception {
    File theFile = new File(path);
    String parent = theFile.getParent();
    return listFilesInHead(parent).stream().anyMatch(f -> theFile.getName().equals(f));
  }

  @Override
  public void deleteFile(String path) throws Exception {
    throw new OperationNotSupportedException();
  }

  @Override
  public String getVersionId() {
    return versionName;
  }

  @Override
  public void pullRepository() {
    jGitService.cloneRepo(versionName);
  }

  private List<String> listFilesInHead(String path) throws Exception {
    return jGitService.getFilesInPath(versionName, path);
  }
}
