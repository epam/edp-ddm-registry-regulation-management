/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.management.validator.format;

import com.epam.digital.data.platform.management.exception.FileExtensionException;
import com.epam.digital.data.platform.management.model.ValidationResult;
import com.epam.digital.data.platform.management.validator.Validator;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

public class FileExtensionValidator extends Validator {

  private final String fileExtension;

  public FileExtensionValidator(String fileExtension) {
    this.fileExtension = fileExtension;
  }

  @Override
  public void selfValidate(MultipartFile inputFile, ValidationResult validationResult) {
    if (!StringUtils.equalsIgnoreCase(
        FilenameUtils.getExtension(inputFile.getOriginalFilename()), fileExtension)) {
      throw new FileExtensionException("Wrong or missed file extension, should be: " + fileExtension);
    }

    validationResult.setExtension(fileExtension);
  }
}