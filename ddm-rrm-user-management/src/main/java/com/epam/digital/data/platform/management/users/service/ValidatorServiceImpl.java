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

package com.epam.digital.data.platform.management.users.service;

import com.epam.digital.data.platform.management.users.model.ValidationResult;
import com.epam.digital.data.platform.management.users.validator.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ValidatorServiceImpl implements ValidatorService {

  private final Validator validator;

  @Override
  @NonNull
  public ValidationResult validate(@NonNull MultipartFile file) {
    var validationResult = new ValidationResult();

    validator.validate(file, validationResult);

    return validationResult;
  }
}