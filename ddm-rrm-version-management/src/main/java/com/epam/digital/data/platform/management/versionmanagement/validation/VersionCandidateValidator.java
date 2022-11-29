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

package com.epam.digital.data.platform.management.versionmanagement.validation;

import com.epam.digital.data.platform.management.gerritintegration.model.CreateChangeInputDto;
import java.util.regex.Pattern;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class VersionCandidateValidator implements ConstraintValidator<VersionCandidate, CreateChangeInputDto> {
  private static final String VALID_NAME_REGEXP = "^[0-9a-zа-щьюяґєіїыэъ-]{3,32}$";
  private static final String VALID_DESCRIPTION_REGEXP = "^[^\"]{0,512}$";

  @Override
  public boolean isValid(CreateChangeInputDto value, ConstraintValidatorContext context) {
    return Pattern.matches(VALID_NAME_REGEXP, value.getName())
        && Pattern.matches(VALID_DESCRIPTION_REGEXP, value.getDescription());
  }
}
