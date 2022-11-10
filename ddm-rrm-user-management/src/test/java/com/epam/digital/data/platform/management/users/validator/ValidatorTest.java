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

import com.epam.digital.data.platform.management.users.exception.FileEncodingException;
import com.epam.digital.data.platform.management.users.exception.FileExtensionException;
import com.epam.digital.data.platform.management.users.exception.FileLoadProcessingException;
import com.epam.digital.data.platform.management.users.model.ValidationResult;
import com.epam.digital.data.platform.management.users.validator.Validator;
import com.epam.digital.data.platform.management.users.validator.format.FileEncodingValidator;
import com.epam.digital.data.platform.management.users.validator.format.FileExtensionValidator;
import com.epam.digital.data.platform.management.users.validator.generic.FileExistenceValidator;
import com.epam.digital.data.platform.management.users.validator.generic.FileNameValidator;
import lombok.SneakyThrows;
import org.apache.any23.encoding.EncodingDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@ExtendWith(MockitoExtension.class)
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
    assertEquals(fileName, validationResult.getFileName());
    assertEquals(encoding, validationResult.getEncoding());
    assertEquals("csv", validationResult.getExtension());
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
      assertEquals(fileName, validationResult.getFileName());
    }

    @Test
    void shouldThrowFileLoadProcessingException() {
      var exception = assertThrows(FileLoadProcessingException.class,
          () -> validator.validate(file, new ValidationResult()));

      assertThat(exception.getMessage()).isEqualTo("File cannot be saved to Ceph - file name is missed");
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

      var exception = assertThrows(FileLoadProcessingException.class,
          () -> validator.validate(file, new ValidationResult()));

      assertThat(exception.getMessage()).isEqualTo("File cannot be saved to Ceph - file is null or empty");
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
      assertEquals(encoding, validationResult.getEncoding());
    }

    @Test
    @SneakyThrows
    void shouldThrowFileEncodingException() {
      var stream = new ByteArrayInputStream("test".getBytes(StandardCharsets.ISO_8859_1));
      when(file.getInputStream()).thenReturn(stream);
      when(encodingDetector.guessEncoding(stream)).thenReturn("ISO_8859_1");

      var exception = assertThrows(FileEncodingException.class,
          () -> validator.validate(file, new ValidationResult()));

      verify(file).getInputStream();
      assertThat(exception.getMessage()).isEqualTo("Wrong file encoding, should be UTF-8");
    }

    @Test
    @SneakyThrows
    void shouldThrowFileLoadProcessingException() {
      var stream = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
      when(file.getInputStream()).thenReturn(stream);
      when(encodingDetector.guessEncoding(stream)).thenThrow(new RuntimeException());

      var exception = assertThrows(FileLoadProcessingException.class,
          () -> validator.validate(file, new ValidationResult()));

      verify(file).getInputStream();
      assertThat(exception.getMessage()).isEqualTo("Error during encoding validation");
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
      assertEquals("csv", validationResult.getExtension());
    }

    @Test
    void shouldThrowFileExtensionException() {
      when(file.getOriginalFilename()).thenReturn("test.txt");

      var exception = assertThrows(FileExtensionException.class,
          () -> validator.validate(file, new ValidationResult()));

      assertThat(exception.getMessage()).isEqualTo("Wrong or missed file extension, should be: csv");
    }
  }
}