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

package com.epam.digital.data.platform.management.users.validator;

import com.epam.digital.data.platform.management.users.model.ValidationResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

public abstract class Validator {

  private Validator nextValidator;

  public Validator linkWith(Validator next) {
    this.nextValidator = next;
    return next;
  }

  public void validate(MultipartFile inputFile, ValidationResult validationResult) {
    selfValidate(inputFile, validationResult);

    if (Objects.nonNull(nextValidator)) {
      nextValidator.validate(inputFile, validationResult);
    }
  }

  protected abstract void selfValidate(MultipartFile inputFile, ValidationResult validationResult);
}