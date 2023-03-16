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

package com.epam.digital.data.platform.management.validation.businessProcess;

import static org.mockito.ArgumentMatchers.anyString;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.validation.ConstraintValidatorContext;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class BusinessProcessValidatorTest {

  @Captor
  private ArgumentCaptor<String> captor;

  private final BusinessProcessValidator businessProcessValidator = new BusinessProcessValidator();

  private ConstraintValidatorContext context;

  @BeforeEach
  public void setUp() {
    context = Mockito.mock(ConstraintValidatorContext.class);
    var builder = Mockito.mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);

    Mockito.when(context.buildConstraintViolationWithTemplate(anyString()))
        .thenReturn(builder);
  }

  @Test
  @SneakyThrows
  void testBpValidationCorrectBpTest() {
    var content = Files.readString(getFile("bp-correct.xml").toPath(), StandardCharsets.UTF_8);
    businessProcessValidator.isValid(content, context);
    Mockito.verify(context, Mockito.never()).buildConstraintViolationWithTemplate("messageTemplate");
  }

  @Test
  @SneakyThrows
  void testBpValidationIncorrectTagInBpTest() {
    var content = Files.readString(getFile("bp-incorrect-tag.xml").toPath(),
        StandardCharsets.UTF_8);
    businessProcessValidator.isValid(content, context);
    Mockito.verify(context).buildConstraintViolationWithTemplate(captor.capture());
    Assertions.assertThat(captor.getValue()).contains("'someTag' is not allowed");
  }

  @Test
  @SneakyThrows
  void testBpValidationEmptyProcessNameTest() {
    var content = Files.readString(getFile("bp-processName-empty.xml").toPath(),
        StandardCharsets.UTF_8);
    businessProcessValidator.isValid(content, context);
    Mockito.verify(context).buildConstraintViolationWithTemplate(captor.capture());
    Assertions.assertThat(captor.getValue()).contains(
        "Value '' with length = '0' is not facet-valid with respect to minLength '1' for type '#AnonType_nametCallableElement'.");
  }

  @Test
  @SneakyThrows
  void testBpValidationNullProcessNameTest() {
    var content = Files.readString(getFile("bp-processName-null.xml").toPath(),
        StandardCharsets.UTF_8);
    businessProcessValidator.isValid(content, context);
    Mockito.verify(context).buildConstraintViolationWithTemplate(captor.capture());
    Assertions.assertThat(captor.getValue())
        .contains("Attribute 'name' must appear on element 'bpmn:process'");
  }

  @Test
  @SneakyThrows
  void testBpValidationIncorrectDatesInBpTest() {
    var content = Files.readString(getFile("bp-incorrect-dates.xml").toPath(),
        StandardCharsets.UTF_8);
    businessProcessValidator.isValid(content, context);
    Mockito.verify(context).buildConstraintViolationWithTemplate(captor.capture());
    Assertions.assertThat(captor.getValue()).contains("'' is not a valid value for 'dateTime'");
  }

  @Test
  @SneakyThrows
  void testBpValidationWithoutDatesInBpTest() {
    var content = Files.readString(getFile("bp-no-dates.xml").toPath(),
        StandardCharsets.UTF_8);
    businessProcessValidator.isValid(content, context);
    Mockito.verify(context, Mockito.never()).buildConstraintViolationWithTemplate(anyString());
    //check if there is no error, but not real value
  }

  private File getFile(String location) {
    return new File(getClass().getClassLoader().getResource(location).getFile());
  }
}
