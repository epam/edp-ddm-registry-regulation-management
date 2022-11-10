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

package com.epam.digital.data.platform.management.users.config;

import com.epam.digital.data.platform.management.users.validator.generic.FileExistenceValidator;
import com.epam.digital.data.platform.management.users.validator.generic.FileNameValidator;
import com.epam.digital.data.platform.management.users.validator.Validator;
import com.epam.digital.data.platform.management.users.validator.format.FileEncodingValidator;
import com.epam.digital.data.platform.management.users.validator.format.FileExtensionValidator;
import org.apache.any23.encoding.EncodingDetector;
import org.apache.any23.encoding.TikaEncodingDetector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidatorConfig {

  @Bean
  public Validator fileValidator(@Value("${file-validator.encoding}") String encoding,
      @Value("${file-validator.extension}") String extension) {
    var validator = new FileExistenceValidator();

    validator.linkWith(new FileNameValidator())
        .linkWith(new FileEncodingValidator(encodingDetector(), encoding))
        .linkWith(new FileExtensionValidator(extension));

    return validator;
  }

  @Bean
  public EncodingDetector encodingDetector() {
    return new TikaEncodingDetector();
  }
}