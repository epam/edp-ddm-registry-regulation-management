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

package com.epam.digital.data.platform.management.users.validator.generic;

import com.epam.digital.data.platform.management.users.exception.FileLoadProcessingException;
import com.epam.digital.data.platform.management.users.model.ValidationResult;
import com.epam.digital.data.platform.management.users.validator.Validator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

public class FileNameValidator extends Validator {

  @Override
  public void selfValidate(MultipartFile inputFile, ValidationResult validationResult) {
    var originalFilename = inputFile.getOriginalFilename();

    if (StringUtils.isBlank(originalFilename)) {
      throw new FileLoadProcessingException("File cannot be saved to Ceph - file name is missed");
    }

    validationResult.setFileName(originalFilename);
  }
}