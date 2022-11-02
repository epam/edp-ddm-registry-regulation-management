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
package com.epam.digital.data.platform.management.gitintegration.service;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.gitintegration.exception.GitCommandException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GitFileService {

  @Autowired
  private GerritPropertiesConfig config;

  @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
  public File writeFile(String repositoryName, String fileContent, String filePath) {
    if (fileContent != null) {
      var repositoryDirectory = FilenameUtils.normalizeNoEndSeparator(
          config.getRepositoryDirectory());
      var fileDirectory = FilenameUtils.getPathNoEndSeparator(filePath);
      var fullPath = repositoryDirectory + File.separator +
          repositoryName + File.separator + fileDirectory;

      var file = new File(FilenameUtils.normalizeNoEndSeparator(fullPath),
          FilenameUtils.getName(filePath));
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
        writer.write(fileContent);
      } catch (IOException e) {
        throw new GitCommandException(
            String.format("Exception occurred during writing content to file %s: %s", filePath,
                e.getMessage()), e);
      }
      return file;
    }
    return null;
  }
}
