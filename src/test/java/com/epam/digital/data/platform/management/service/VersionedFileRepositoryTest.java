package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.epam.digital.data.platform.management.model.dto.FileResponse;
import com.epam.digital.data.platform.management.model.dto.FileStatus;
import com.epam.digital.data.platform.management.model.dto.VersioningRequestDto;
import com.epam.digital.data.platform.management.service.impl.VersionedFileRepositoryImpl;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.FileInfo;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class VersionedFileRepositoryTest {
    @Mock
    private JGitService jGitService;
    @Mock
    private GerritService gerritService;
    @InjectMocks
    private VersionedFileRepositoryImpl repository;

    @Test
    @SneakyThrows
    void getFileListTest() {
        List<String> list = new ArrayList<>();
        list.add("file1");
        list.add("file2");
        Mockito.when(jGitService.getFilesInPath(any(), eq(File.separator))).thenReturn(list);
        List<FileResponse> fileList = repository.getFileList(File.separator);
        Assertions.assertNotNull(fileList);
    }

    @Test
    @SneakyThrows
    void getRootFileListTest() {
        List<String> list = new ArrayList<>();
        list.add("file1");
        list.add("file2");
        Mockito.when(jGitService.getFilesInPath(any(), eq(File.separator))).thenReturn(list);
        List<FileResponse> fileList = repository.getFileList();
        Assertions.assertNotNull(fileList);
    }
    @Test
    @SneakyThrows
    void getVersionedFileListTest() {
        List<String> list = new ArrayList<>();
        list.add("folder/file1");
        list.add("folder/file2");
        list.add("folder/file3");
        var changeInfo = new ChangeInfo();
        changeInfo.created = new Timestamp(System.currentTimeMillis());
        var filesInMR = new HashMap<String, FileInfo>();
        filesInMR.put("folder/file22", new FileInfo());
        filesInMR.put("folder/file3", new FileInfo());

        Mockito.when(gerritService.getMRByNumber(any())).thenReturn(changeInfo);
        Mockito.when(gerritService.getListOfChangesInMR(any())).thenReturn(filesInMR);
        Mockito.when(jGitService.getFilesInPath(any(), eq("folder"))).thenReturn(list);
        List<FileResponse> fileList = repository.getFileList("folder");
        Assertions.assertNotNull(fileList);
        Assertions.assertEquals(4, fileList.size());
    }

  @Test
  @SneakyThrows
  void getVersionedFileListWithStatusesTest() {
    List<String> list = new ArrayList<>();
    list.add("file1");
    list.add("file2");
    list.add("file3");
    var changeInfo = new ChangeInfo();
    changeInfo.created = new Timestamp(System.currentTimeMillis());
    var filesInMR = new HashMap<String, FileInfo>();
    var fileInfo = new FileInfo();
    fileInfo.status = 'A';
    filesInMR.put("folder/file12", fileInfo);
    fileInfo = new FileInfo();
    fileInfo.status = 'D';
    filesInMR.put("folder/file2", fileInfo);
    filesInMR.put("folder/file14", new FileInfo());
    filesInMR.put("folder/file3", new FileInfo());

    Mockito.when(gerritService.getMRByNumber(any())).thenReturn(changeInfo);
    Mockito.when(gerritService.getListOfChangesInMR(any())).thenReturn(filesInMR);
    Mockito.when(jGitService.getFilesInPath(any(), eq("folder"))).thenReturn(list);
    List<FileResponse> fileList = repository.getFileList("folder");
    Assertions.assertNotNull(fileList);
    Assertions.assertEquals(5, fileList.size());
    Assertions.assertEquals(FileStatus.CURRENT, getFileStatusByName(fileList, "file1"));
    Assertions.assertEquals(FileStatus.DELETED, getFileStatusByName(fileList, "file2"));
    Assertions.assertEquals(FileStatus.CHANGED, getFileStatusByName(fileList, "file3"));
    Assertions.assertEquals(FileStatus.NEW, getFileStatusByName(fileList, "file12"));
    Assertions.assertEquals(FileStatus.NEW, getFileStatusByName(fileList, "file14"));
  }

  private FileStatus getFileStatusByName(List<FileResponse> files, String name) {
      return files.stream()
          .filter(e -> name.equals(e.getName()))
          .findAny()
          .map(FileResponse::getStatus)
          .orElse(null);
  }

    @Test
    @SneakyThrows
    void writeFileTest() {
        ChangeInfoDto changeInfoDto = new ChangeInfoDto();
        changeInfoDto.setSubject("change");
        repository.setVersionName("1");
        List<ChangeInfo> changes = new ArrayList<>();
        ChangeInfo changeInfo = new ChangeInfo();
        changeInfo.changeId = "changeId";
        changeInfo._number = 1;
        changes.add(changeInfo);
        Mockito.when(gerritService.getChangeInfo("changeId")).thenReturn(changeInfoDto);
        Mockito.when(jGitService.amend(any(), any())).thenReturn("");
        Mockito.when(gerritService.getMRByNumber(eq("1"))).thenReturn(changeInfo);
        repository.writeFile("/form", "content");
        VersioningRequestDto content = VersioningRequestDto.builder().versionName("1").formName("form").content("content").build();
        Mockito.verify(gerritService, Mockito.times(1)).getChangeInfo("changeId");
        Mockito.verify(jGitService, Mockito.times(1)).amend(any(), any());
    }

    @Test
    @SneakyThrows
    void deleteTest() {
        repository.setVersionName("1");
        String deleted = repository.deleteFile("form");
        Assertions.assertEquals("File does not exist", deleted);
        List<ChangeInfo> changes = new ArrayList<>();
        ChangeInfo changeInfo = new ChangeInfo();
        changeInfo.changeId = "changeId";
        changeInfo._number = 1;
        changes.add(changeInfo);
        ChangeInfoDto changeInfoDto = new ChangeInfoDto();
        changeInfoDto.setSubject("change");
        Mockito.when(gerritService.getMRByNumber(eq("1"))).thenReturn(changeInfo);
        Mockito.when(gerritService.getChangeInfo("changeId")).thenReturn(changeInfoDto);
        Mockito.when(jGitService.delete(any(), any())).thenReturn("deleted");
        String form = repository.deleteFile("form");
        Assertions.assertEquals("deleted", form);
    }

    @Test
    @SneakyThrows
    void readFileTest() {
        Mockito.when(gerritService.getFileContent(any(), any())).thenReturn("");
        String file = repository.readFile("/");
        Assertions.assertNotNull(file);
        Mockito.verify(gerritService, Mockito.times(1)).getFileContent(any(), any());
    }

    @Test
    @SneakyThrows
    void pullRepositoryTest() {
        repository.setVersionName("version");
        repository.pullRepository();
        Mockito.verify(jGitService, Mockito.times(1)).cloneRepo("version");
    }

    @Test
    @SneakyThrows
    void isFileExistsTest() {
        ArrayList<String> t = new ArrayList<>();
        t.add("fileName");
        Mockito.when(jGitService.getFilesInPath(any(), any())).thenReturn(t);
        boolean fileExists = repository.isFileExists("/fileName");
        Assertions.assertEquals(true, fileExists);
    }
}
