package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.model.dto.FileResponse;
import com.epam.digital.data.platform.management.model.dto.FileStatus;
import com.epam.digital.data.platform.management.model.dto.FormResponse;
import com.epam.digital.data.platform.management.service.impl.FormServiceImpl;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class FormServiceTest {

  private static final String contentForm = "{\n" +
      "  \"type\": \"form\",\n" +
      "  \"components\": [\n" +
      "   ],\n" +
      "  \"title\": \"Внести фізичні фактори1\",\n" +
      "  \"path\": \"add-fizfactors1\",\n" +
      "  \"name\": \"add-fizfactors1\",\n" +
      "  \"display\": \"form\",\n" +
      "  \"submissionAccess\": []\n" +
      "}";
  @Mock
  private VersionedFileRepositoryFactory repositoryFactory;
  @Mock
  private VersionedFileRepository repository;
  @InjectMocks
  private FormServiceImpl formService;
  private List<FileResponse> forms = new ArrayList<>();

  @BeforeEach
  @SneakyThrows
  private void beforeEach() {
    forms.add(FileResponse.builder().name("form").status(FileStatus.NEW).build());
    Mockito.when(repositoryFactory.getRepoByVersion(any())).thenReturn(repository);
  }

  @Test
  @SneakyThrows
  void getFormListByVersionTest() {
    Mockito.when(repository.getFileList(any())).thenReturn(forms);
    Mockito.when(repository.readFile(any())).thenReturn(contentForm);
    List<FormResponse> version = formService.getFormListByVersion("form");
    Assertions.assertNotNull(version);
    Assertions.assertEquals("form", version.get(0).getName());
    Assertions.assertEquals(FileStatus.NEW, version.get(0).getStatus());
  }

  @Test
  @SneakyThrows
  void createFormTestNoErrorTest() {
    formService.createForm("form", "content", "version");
  }

  @Test
  @SneakyThrows
  void getFormContentTest() {
    Mockito.when(repository.readFile(any())).thenReturn(anyString());
    String formContent = formService.getFormContent("form", "version");
    Assertions.assertNotNull(formContent);
  }

  @Test
  @SneakyThrows
  void updateFormTestNoErrorTest() {
    formService.updateForm("content", "form", "version");
  }

  @Test
  @SneakyThrows
  void deleteFormTest() {
    Mockito.when(repository.deleteFile(any())).thenReturn(anyString());
    formService.deleteForm("form", "version");
  }
}
