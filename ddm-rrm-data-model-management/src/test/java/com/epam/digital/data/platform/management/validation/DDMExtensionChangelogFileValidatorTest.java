/*
 * Copyright 2023 EPAM Systems.
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

package com.epam.digital.data.platform.management.validation;

import static org.mockito.ArgumentMatchers.anyString;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.validation.ConstraintValidatorContext;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.ResourceUtils;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DDMExtensionChangelogFileValidator.class)
class DDMExtensionChangelogFileValidatorTest {

  @Autowired
  DDMExtensionChangelogFileValidator validator;
  @Mock
  ConstraintValidatorContext context;

  @BeforeEach
  void setUp() {
    var builder = Mockito.mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

    Mockito.doReturn(builder).when(context).buildConstraintViolationWithTemplate(anyString());
  }

  @Test
  @SneakyThrows
  void happyPath() {
    var content = Files.readString(
        ResourceUtils.getFile("classpath:changelog-correct.xml").toPath(),
        StandardCharsets.UTF_8);
    var result = validator.isValid(content, context);
    Assertions.assertThat(result).isTrue();
    Mockito.verify(context, Mockito.never()).buildConstraintViolationWithTemplate(anyString());
  }

  @Test
  @SneakyThrows
  void shouldReturnFalseIfNotValid() {
    var content = Files.readString(
        ResourceUtils.getFile("classpath:changelog-incorrect.xml").toPath(),
        StandardCharsets.UTF_8);
    var result = validator.isValid(content, context);
    Assertions.assertThat(result).isFalse();
    Mockito.verify(context).buildConstraintViolationWithTemplate(
        "cvc-elt.1.a: Cannot find the declaration of element 'databaseBrokenChangeLog'.");
  }
}
