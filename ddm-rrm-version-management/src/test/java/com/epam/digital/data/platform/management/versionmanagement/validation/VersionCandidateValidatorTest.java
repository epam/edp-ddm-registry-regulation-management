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

import static org.mockito.ArgumentMatchers.anyString;

import com.epam.digital.data.platform.management.gerritintegration.model.CreateChangeInputDto;
import java.util.stream.Stream;
import javax.validation.ConstraintValidatorContext;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class VersionCandidateValidatorTest {

  private final VersionCandidateValidator validator = new VersionCandidateValidator();
  private ConstraintValidatorContext context;
  private static final int MAX_NAME_LENGTH = 32;
  private static final int MIN_NAME_LENGTH = 3;
  private static final int MAX_DESCRIPTION_LENGTH = 512;

  @BeforeEach
  public void setUp() {
    context = Mockito.mock(ConstraintValidatorContext.class);
    var builder = Mockito.mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

    Mockito.when(context.buildConstraintViolationWithTemplate(anyString()))
        .thenReturn(builder);
  }

  @Test
  @SneakyThrows
  void validVersionCandidateTest() {
    var validVersionCandidate = CreateChangeInputDto.builder()
        .name("valid-name")
        .description("fine-description123")
        .build();
    Assertions.assertThat(validator.isValid(validVersionCandidate, context)).isTrue();
  }

  @SneakyThrows
  @ParameterizedTest
  @MethodSource("provideInvalidNames")
  void invalidCandidateNameTest(String name) {
    var invalidVersionCandidate = CreateChangeInputDto.builder()
        .name(name)
        .description("valid description").build();
    Assertions.assertThat(validator.isValid(invalidVersionCandidate, context)).isFalse();
  }

  @SneakyThrows
  @ParameterizedTest
  @MethodSource("provideInvalidDescriptions")
  void invalidCandidateDescriptionTest(String description) {
    var invalidVersionCandidate = CreateChangeInputDto.builder()
        .description(description)
        .name("valid-name").build();
    Assertions.assertThat(validator.isValid(invalidVersionCandidate, context)).isFalse();
  }

  private static Stream<String> provideInvalidNames() {
    return Stream.of("INVALID_NAME", RandomString.make(MAX_NAME_LENGTH + 1),
        RandomString.make(MIN_NAME_LENGTH - 1));
  }

  private static Stream<String> provideInvalidDescriptions() {
    return Stream.of("\"Quotes are forbidden in description\"",
        RandomString.make(MAX_DESCRIPTION_LENGTH + 1));
  }
}
