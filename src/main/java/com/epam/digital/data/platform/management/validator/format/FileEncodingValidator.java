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

import com.epam.digital.data.platform.management.exception.FileEncodingException;
import com.epam.digital.data.platform.management.exception.FileLoadProcessingException;
import com.epam.digital.data.platform.management.model.ValidationResult;
import com.epam.digital.data.platform.management.validator.Validator;
import org.apache.any23.encoding.EncodingDetector;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

public class FileEncodingValidator extends Validator {

  private final EncodingDetector encodingDetector;
  private final String fileEncoding;

  public FileEncodingValidator(EncodingDetector encodingDetector, String fileEncoding) {
    this.encodingDetector = encodingDetector;
    this.fileEncoding = fileEncoding;
  }

  @Override
  public void selfValidate(MultipartFile inputFile, ValidationResult validationResult) {
    validateCharset(inputFile);

    validationResult.setEncoding(fileEncoding);
  }

  private void validateCharset(MultipartFile inputFile) {

    String guessedEncoding;
    try {
      guessedEncoding = encodingDetector.guessEncoding(inputFile.getInputStream());
    } catch (Exception e) {
      throw new FileLoadProcessingException("Error during encoding validation", e);
    }

    if (!StringUtils.equals(fileEncoding, guessedEncoding)) {
      throw new FileEncodingException("Wrong file encoding, should be " + fileEncoding);
    }
  }
}