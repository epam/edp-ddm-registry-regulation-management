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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.management.users.exception.FileEncodingException;
import com.epam.digital.data.platform.management.users.exception.FileExtensionException;
import com.epam.digital.data.platform.management.users.exception.FileLoadProcessingException;
import com.epam.digital.data.platform.management.users.model.ValidationResult;
import com.epam.digital.data.platform.management.users.validator.format.FileEncodingValidator;
import com.epam.digital.data.platform.management.users.validator.format.FileExtensionValidator;
import com.epam.digital.data.platform.management.users.validator.generic.FileExistenceValidator;
import com.epam.digital.data.platform.management.users.validator.generic.FileNameValidator;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import org.apache.any23.encoding.EncodingDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(SpringExtension.class)
class ValidatorTest {

  @Mock
  MockMultipartFile file;

  @Mock
  EncodingDetector encodingDetector;

  ValidationResult validationResult;

  Validator validator;

  @BeforeEach
  void init() {
    validator = new FileExistenceValidator();

    validator.linkWith(new FileNameValidator())
        .linkWith(new FileEncodingValidator(encodingDetector, "UTF-8"))
        .linkWith(new FileExtensionValidator("csv"));

    validationResult = new ValidationResult();
  }

  @Test
  @SneakyThrows
  void shouldSuccessfullyValidateChainWithNoErrors() {
    final var fileName = "test.csv";
    final var encoding = "UTF-8";
    var stream = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
    when(file.isEmpty()).thenReturn(false);
    when(file.getInputStream()).thenReturn(stream);
    when(encodingDetector.guessEncoding(stream)).thenReturn(encoding);
    when(file.getOriginalFilename()).thenReturn(fileName);

    validator.validate(file, validationResult);

    verify(file).isEmpty();
    verify(file).getInputStream();
    verify(file, times(2)).getOriginalFilename();
    assertThat(validationResult.getFileName()).isEqualTo(fileName);
    assertThat(validationResult.getEncoding()).isEqualTo(encoding);
    assertThat(validationResult.getExtension()).isEqualTo("csv");
  }


  @Nested
  class FileNameValidatorTest {

    @BeforeEach
    void init() {
      validator = new FileNameValidator();
    }

    @Test
    void shouldSuccessfullyValidateWithNoErrors() {
      final var fileName = "test.csv";
      validationResult = new ValidationResult();
      when(file.getOriginalFilename()).thenReturn(fileName);

      validator.validate(file, validationResult);

      verify(file).getOriginalFilename();
      assertThat(validationResult.getFileName()).isEqualTo(fileName);
    }

    @Test
    void shouldThrowFileLoadProcessingException() {
      assertThatCode(() -> validator.validate(file, new ValidationResult()))
          .isInstanceOf(FileLoadProcessingException.class)
          .hasMessage("File cannot be saved to Ceph - file name is missed");
    }
  }

  @Nested
  class FileExistenceValidatorTest {

    @BeforeEach
    void init() {
      validator = new FileExistenceValidator();
    }

    @Test
    void shouldSuccessfullyValidateWithNoErrors() {
      when(file.isEmpty()).thenReturn(false);

      validator.validate(file, new ValidationResult());

      verify(file).isEmpty();
    }

    @Test
    void shouldThrowFileLoadProcessingException() {
      when(file.isEmpty()).thenReturn(true);

      assertThatCode(() -> validator.validate(file, new ValidationResult()))
          .isInstanceOf(FileLoadProcessingException.class)
          .hasMessage("File cannot be saved to Ceph - file is null or empty");
    }
  }

  @Nested
  class FileEncodingValidatorTest {

    @Mock
    EncodingDetector encodingDetector;

    @BeforeEach
    void init() {
      validator = new FileEncodingValidator(encodingDetector, StandardCharsets.UTF_8.name());
    }

    @Test
    @SneakyThrows
    void shouldSuccessfullyValidateWithNoErrors() {
      var encoding = "UTF-8";
      validationResult = new ValidationResult();
      var stream = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
      when(file.getInputStream()).thenReturn(stream);
      when(encodingDetector.guessEncoding(stream)).thenReturn(encoding);

      validator.validate(file, validationResult);

      verify(file).getInputStream();
      assertThat(validationResult.getEncoding()).isEqualTo(encoding);
    }

    @Test
    @SneakyThrows
    void shouldThrowFileEncodingException() {
      var stream = new ByteArrayInputStream("test".getBytes(StandardCharsets.ISO_8859_1));
      when(file.getInputStream()).thenReturn(stream);
      when(encodingDetector.guessEncoding(stream)).thenReturn("ISO_8859_1");

      assertThatCode(() -> validator.validate(file, new ValidationResult()))
          .isInstanceOf(FileEncodingException.class)
          .hasMessage("Wrong file encoding, should be UTF-8");

      verify(file).getInputStream();
    }

    @Test
    @SneakyThrows
    void shouldThrowFileLoadProcessingException() {
      var stream = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
      when(file.getInputStream()).thenReturn(stream);
      when(encodingDetector.guessEncoding(stream)).thenThrow(new RuntimeException());

      assertThatCode(() -> validator.validate(file, new ValidationResult()))
          .isInstanceOf(FileLoadProcessingException.class)
          .hasMessage("Error during encoding validation");

      verify(file).getInputStream();
    }
  }

  @Nested
  class FileExtensionValidatorTest {

    @BeforeEach
    void init() {
      validator = new FileExtensionValidator("csv");
    }

    @Test
    void shouldSuccessfullyValidateWithNoErrors() {
      var fileName = "test.csv";
      validationResult = new ValidationResult();
      when(file.getOriginalFilename()).thenReturn(fileName);

      validator.validate(file, validationResult);

      verify(file).getOriginalFilename();
      assertThat(validationResult.getExtension()).isEqualTo("csv");
    }

    @Test
    void shouldThrowFileExtensionException() {
      when(file.getOriginalFilename()).thenReturn("test.txt");

      assertThatCode(() -> validator.validate(file, new ValidationResult()))
          .isInstanceOf(FileExtensionException.class)
          .hasMessage("Wrong or missed file extension, should be: csv");

    }
  }
}