package com.epam.digital.data.platform.management.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.exception.FormAlreadyExistsException;
import com.epam.digital.data.platform.management.model.dto.FileResponse;
import com.epam.digital.data.platform.management.model.dto.FileStatus;
import com.epam.digital.data.platform.management.model.dto.FormResponse;
import com.epam.digital.data.platform.management.service.impl.FormServiceImpl;
import java.time.LocalDateTime;
import java.util.List;

import liquibase.pro.packaged.S;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

@ExtendWith(MockitoExtension.class)
class FormServiceTest {

  private static final String VERSION_ID = "version";

  @Captor
  private ArgumentCaptor<String> captor;
  private static final String FORM_CONTENT = "{\n" +
      "  \"type\": \"form\",\n" +
      "  \"components\": [],\n" +
      "  \"title\": \"Внести фізичні фактори1\",\n" +
      "  \"path\": \"add-fizfactors1\",\n" +
      "  \"name\": \"add-fizfactors1\",\n" +
      "  \"display\": \"form\",\n" +
      "  \"submissionAccess\": [],\n" +
      "  \"created\": \"" + LocalDateTime.now() + "\",\n" +
      "  \"modified\": \"" + LocalDateTime.now() + "\"\n" +
      "}";

  @Mock
  private VersionedFileRepositoryFactory repositoryFactory;
  @Mock
  private VersionedFileRepository repository;
  @Mock
  private GerritPropertiesConfig gerritPropertiesConfig;
  @InjectMocks
  private FormServiceImpl formService;

  @BeforeEach
  @SneakyThrows
  void beforeEach() {
    Mockito.when(repositoryFactory.getRepoByVersion(VERSION_ID)).thenReturn(repository);
  }

  @Test
  @SneakyThrows
  void getFormListByVersionTest() {
    var newForm = FileResponse.builder()
        .name("form")
        .path("forms/form.json")
        .status(FileStatus.NEW)
        .created(LocalDateTime.of(2022, 8, 10, 13, 18))
        .updated(LocalDateTime.of(2022, 8, 10, 13, 28))
        .build();
    var deletedForm = FileResponse.builder().status(FileStatus.DELETED).build();
    Mockito.when(repository.getFileList("forms")).thenReturn(List.of(newForm, deletedForm));
    Mockito.when(repository.readFile("forms/form.json")).thenReturn(FORM_CONTENT);

    var resultList = formService.getFormListByVersion(VERSION_ID);

    var expectedFormResponseDto = FormResponse.builder()
        .name("form")
        .path("forms/form.json")
        .status(FileStatus.NEW)
        .created(LocalDateTime.of(2022, 8, 10, 13, 18))
        .updated(LocalDateTime.of(2022, 8, 10, 13, 28))
        .title("Внести фізичні фактори1")
        .build();
    Assertions.assertThat(resultList)
        .hasSize(1)
        .element(0).isEqualTo(expectedFormResponseDto);
  }

  @Test
  @SneakyThrows
  void createFormTestNoErrorTest() {
    Mockito.when(repository.isFileExists("forms/form.json")).thenReturn(false);

    Assertions.assertThatCode(() -> formService.createForm("form", FORM_CONTENT, VERSION_ID))
        .doesNotThrowAnyException();

    Mockito.verify(repository).writeFile(Mockito.eq("forms/form.json"), captor.capture());
    var response = captor.getValue();
    JSONAssert.assertEquals(FORM_CONTENT, response, new CustomComparator(JSONCompareMode.LENIENT,
        new Customization("modified", (o1, o2) -> true),
        new Customization("created", (o1, o2) -> true)
    ));
  }


  @Test
  @SneakyThrows
  void createFormConflictTest() {
    Mockito.when(repository.isFileExists("forms/formName.json")).thenReturn(true);

    Assertions.assertThatThrownBy(() -> formService.createForm("formName", "content", VERSION_ID))
        .isInstanceOf(FormAlreadyExistsException.class);

    Mockito.verify(repository, never()).writeFile(any(), any());
  }

  @Test
  @SneakyThrows
  void getFormContentTest() {
    Mockito.when(repository.readFile("forms/form.json")).thenReturn(FORM_CONTENT);

    var actualFormContent = formService.getFormContent("form", VERSION_ID);

    Assertions.assertThat(actualFormContent)
        .isEqualTo(FORM_CONTENT);
  }

  @Test
  @SneakyThrows
  void updateFormTestNoErrorTest() {
    Assertions.assertThatCode(() -> formService.updateForm(FORM_CONTENT, "form", VERSION_ID))
        .doesNotThrowAnyException();

    Mockito.verify(repository).writeFile(Mockito.eq("forms/form.json"), captor.capture());
    var response = captor.getValue();
    JSONAssert.assertEquals(FORM_CONTENT, response, new CustomComparator(JSONCompareMode.LENIENT,
        new Customization("modified", (o1, o2) -> true),
        new Customization("created", (o1, o2) -> true)
    ));
  }

  @Test
  @SneakyThrows
  void deleteFormTest() {
    Assertions.assertThatCode(() -> formService.deleteForm("form", VERSION_ID))
        .doesNotThrowAnyException();

    Mockito.verify(repository).deleteFile("forms/form.json");
  }
}
