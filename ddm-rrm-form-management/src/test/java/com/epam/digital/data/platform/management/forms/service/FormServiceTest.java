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

package com.epam.digital.data.platform.management.forms.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;

import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.core.context.VersionContextComponentManager;
import com.epam.digital.data.platform.management.core.service.CacheService;
import com.epam.digital.data.platform.management.filemanagement.model.FileStatus;
import com.epam.digital.data.platform.management.filemanagement.model.VersionedFileDatesDto;
import com.epam.digital.data.platform.management.filemanagement.model.VersionedFileInfoDto;
import com.epam.digital.data.platform.management.filemanagement.service.VersionedFileRepository;
import com.epam.digital.data.platform.management.forms.FormMapper;
import com.epam.digital.data.platform.management.forms.exception.FormAlreadyExistsException;
import com.epam.digital.data.platform.management.forms.exception.FormNotFoundException;
import com.epam.digital.data.platform.management.forms.model.FormInfoDto;
import com.epam.digital.data.platform.management.forms.util.TestUtils;
import java.time.LocalDateTime;
import java.util.List;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class FormServiceTest {

  private static final String VERSION_ID = "version";

  @Captor
  private ArgumentCaptor<String> captor;
  private final String FORM_CONTENT = TestUtils.getContent("form-sample.json");
  private final String FORM_CONTENT_UNICODE = TestUtils.getContent("form-sample-unicode.json");
  private final String FORM_CONTENT_WITHOUT_DATES = TestUtils.getContent(
      "form-sample-without-dates.json");

  @Mock
  private VersionContextComponentManager versionContextComponentManager;
  @Mock
  private VersionedFileRepository repository;
  @Mock
  private VersionedFileRepository masterRepository;
  @Mock
  private GerritPropertiesConfig gerritPropertiesConfig;
  @Mock
  private CacheService cacheService;
  @Spy
  private FormMapper formMapper = Mappers.getMapper(FormMapper.class);
  @InjectMocks
  private FormServiceImpl formService;

  @BeforeEach
  @SneakyThrows
  void beforeEach() {
    Mockito.when(
            versionContextComponentManager.getComponent(VERSION_ID, VersionedFileRepository.class))
        .thenReturn(repository);
    Mockito.when(versionContextComponentManager.getComponent(gerritPropertiesConfig.getHeadBranch(),
        VersionedFileRepository.class)).thenReturn(masterRepository);
  }

  @Test
  @SneakyThrows
  void getFormListByVersionTest() {
    var newForm = VersionedFileInfoDto.builder().name("form").path("forms/form.json")
        .status(FileStatus.NEW)
        .build();
    var deletedForm = VersionedFileInfoDto.builder().status(FileStatus.DELETED).build();
    Mockito.when(repository.getFileList("forms")).thenReturn(List.of(newForm, deletedForm));
    Mockito.when(repository.readFile("forms/form.json")).thenReturn(FORM_CONTENT);
    Mockito.when(cacheService.getConflictsCache(VERSION_ID)).thenReturn(List.of("forms/form.json"));

    var resultList = formService.getFormListByVersion(VERSION_ID);

    var expectedFormResponseDto = FormInfoDto.builder().name("form").path("forms/form.json")
        .status(FileStatus.NEW).created(LocalDateTime.of(2022, 12, 21, 13, 52, 31, 357000000))
        .updated(LocalDateTime.of(2022, 12, 22, 14, 52, 23, 745000000))
        .title("Update physical factors").conflicted(true).build();
    Assertions.assertThat(resultList).hasSize(1).element(0).isEqualTo(expectedFormResponseDto);
  }

  @Test
  @SneakyThrows
  void getFormListByVersionTest_noDatesInFormContent() {
    var newForm = VersionedFileInfoDto.builder()
        .name("form")
        .path("forms/form.json")
        .status(FileStatus.NEW)
        .build();
    var newFormDates = VersionedFileDatesDto.builder()
        .created(LocalDateTime.of(2022, 8, 10, 13, 18))
        .updated(LocalDateTime.of(2022, 8, 10, 13, 28))
        .build();
    var deletedForm = VersionedFileInfoDto.builder().status(FileStatus.DELETED).build();
    Mockito.doReturn(List.of(newForm, deletedForm)).when(repository).getFileList("forms");
    Mockito.doReturn(FORM_CONTENT_WITHOUT_DATES).when(repository).readFile("forms/form.json");
    Mockito.doReturn(List.of("forms/form.json")).when(cacheService).getConflictsCache(VERSION_ID);
    Mockito.doReturn(newFormDates).when(repository).getVersionedFileDates("forms/form.json");

    var resultList = formService.getFormListByVersion(VERSION_ID);

    var expectedFormResponseDto = FormInfoDto.builder().name("form").path("forms/form.json")
        .status(FileStatus.NEW).created(LocalDateTime.of(2022, 8, 10, 13, 18))
        .updated(LocalDateTime.of(2022, 8, 10, 13, 28))
        .title("Update physical factors").conflicted(true).build();
    Assertions.assertThat(resultList).hasSize(1).element(0).isEqualTo(expectedFormResponseDto);
  }

  @Test
  @SneakyThrows
  void getChangedFormsListByVersionTest() {
    var newForm = VersionedFileInfoDto.builder().name("form").path("forms/form.json")
        .status(FileStatus.DELETED)
        .build();
    Mockito.when(repository.getFileList("forms")).thenReturn(List.of(newForm));
    Mockito.when(masterRepository.readFile("forms/form.json")).thenReturn(FORM_CONTENT);

    var resultList = formService.getChangedFormsListByVersion(VERSION_ID);

    var expectedFormResponseDto = FormInfoDto.builder().name("form").path("forms/form.json")
        .status(FileStatus.DELETED).created(LocalDateTime.of(2022, 12, 21, 13, 52, 31, 357000000))
        .updated(LocalDateTime.of(2022, 12, 22, 14, 52, 23, 745000000))
        .title("Update physical factors").build();
    Assertions.assertThat(resultList).hasSize(1).element(0).isEqualTo(expectedFormResponseDto);
  }

  @Test
  @SneakyThrows
  void createFormTestNoErrorTest() {
    Mockito.when(repository.isFileExists("forms/form.json")).thenReturn(false);
    Assertions.assertThatCode(() -> formService.createForm("form", FORM_CONTENT, VERSION_ID))
        .doesNotThrowAnyException();
    Mockito.verify(repository).isFileExists("forms/form.json");
    Mockito.verify(repository).writeFile(eq("forms/form.json"), anyString());
    //check if there is no error, but not real value
  }

  @Test
  @SneakyThrows
  void createFormTest() {
    Mockito.when(repository.isFileExists("forms/form.json")).thenReturn(false);
    formService.createForm("form", FORM_CONTENT, VERSION_ID);
    Mockito.verify(repository).writeFile(eq("forms/form.json"), captor.capture());
    var response = captor.getValue();
    JSONAssert.assertEquals(FORM_CONTENT, response, new CustomComparator(JSONCompareMode.LENIENT,
        new Customization("modified", (o1, o2) -> true),
        new Customization("created", (o1, o2) -> true)));
  }


  @Test
  @SneakyThrows
  void createFormConflictTest() {
    Mockito.when(repository.isFileExists("forms/formName.json")).thenReturn(true);

    Assertions.assertThatThrownBy(() -> formService.createForm("formName", "content", VERSION_ID))
        .isInstanceOf(FormAlreadyExistsException.class);

    Mockito.verify(repository, never()).writeFile("forms/formName.json", "content");
  }

  @Test
  @SneakyThrows
  void getFormContentTest() {
    Mockito.when(repository.readFile("forms/form.json")).thenReturn(FORM_CONTENT);

    var actualFormContent = formService.getFormContent("form", VERSION_ID);

    Assertions.assertThat(actualFormContent).isEqualTo(FORM_CONTENT);
    Mockito.verify(repository).updateRepository();
  }

  @Test
  @SneakyThrows
  void getFormContentNullTest() {
    Mockito.when(repository.readFile("forms/form.json")).thenReturn(null);

    Assertions.assertThatThrownBy(() -> formService.getFormContent("form", VERSION_ID))
        .isInstanceOf(FormNotFoundException.class)
        .hasMessage("Form form not found");
  }

  @Test
  @SneakyThrows
  void updateFormTestNoErrorTest() {
    Assertions.assertThatCode(() -> formService.updateForm(FORM_CONTENT, "form", VERSION_ID, null))
        .doesNotThrowAnyException();
    Mockito.verify(repository).isFileExists("forms/form.json");
    Mockito.verify(repository).writeFile(eq("forms/form.json"), anyString(), isNull());
    //check if there is no error, but not real value
  }

  @Test
  @SneakyThrows
  void updateFormTest() {
    Mockito.when(repository.isFileExists("forms/form.json")).thenReturn(true);
    Mockito.when(repository.readFile("forms/form.json")).thenReturn(FORM_CONTENT);

    String newFormContent = FORM_CONTENT.replaceFirst(
        "\"title\": \"Update physical factors\"",
        "\"title\": \"Update physical factors updated\"");

    formService.updateForm(newFormContent, "form", VERSION_ID, null);
    Mockito.verify(repository).writeFile(eq("forms/form.json"), captor.capture(), isNull());
    var response = captor.getValue();
    JSONAssert.assertEquals(newFormContent, response, new CustomComparator(JSONCompareMode.LENIENT,
        new Customization("modified", (o1, o2) -> true),
        new Customization("created", (o1, o2) -> true)));
  }

  @Test
  @SneakyThrows
  void updateFormTest_noDatesInFileContent() {
    var formName = "form";
    var formPath = "forms/" + formName + ".json";

    Mockito.doReturn(true).when(repository).isFileExists(formPath);
    Mockito.doReturn(FORM_CONTENT_WITHOUT_DATES).when(repository).readFile(formPath);

    var newFormContent = FORM_CONTENT_WITHOUT_DATES.replaceFirst(
        "\"title\": \"Update physical factors\"",
        "\"title\": \"Update physical factors updated\"");

    formService.updateForm(newFormContent, formName, VERSION_ID, null);
    Mockito.verify(repository).writeFile(eq(formPath), captor.capture(), isNull());
    var response = captor.getValue();
    JSONAssert.assertEquals(newFormContent, response, new CustomComparator(JSONCompareMode.LENIENT,
        new Customization("modified", (o1, o2) -> true),
        new Customization("created", (o1, o2) -> true)));

    Mockito.verify(repository).getVersionedFileDates(formPath);
  }

  @Test
  @SneakyThrows
  void deleteFormTest() {
    Assertions.assertThatCode(() -> formService.deleteForm("form", VERSION_ID, "eTag"))
        .doesNotThrowAnyException();

    Mockito.verify(repository).deleteFile("forms/form.json", "eTag");
  }

  @Test
  @SneakyThrows
  void updateFormWhenOnlyModifiedDateChangedTest() {
    Mockito.when(repository.isFileExists("forms/form.json")).thenReturn(true);
    Mockito.when(repository.readFile("forms/form.json")).thenReturn(FORM_CONTENT_UNICODE);

    formService.updateForm(FORM_CONTENT, "form", VERSION_ID, null);
    Mockito.verify(repository, never()).writeFile(eq("forms/form.json"), captor.capture());
  }

  @Test
  @SneakyThrows
  void rollbackFormTest() {
    formService.rollbackForm("form", VERSION_ID);

    Mockito.verify(repository).rollbackFile("forms/form.json");
  }
}
