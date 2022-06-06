package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.model.dto.FileResponse;
import com.epam.digital.data.platform.management.service.impl.HeadFileRepositoryImpl;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.naming.OperationNotSupportedException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class HeadFileRepositoryTest {
    @Mock
    private JGitService jGitService;
    @InjectMocks
    private HeadFileRepositoryImpl repository;

    @Test
    @SneakyThrows
    void getFileListTest() {
        List<String> list = new ArrayList<>();
        Mockito.when(jGitService.getFilesInPath(any(), any())).thenReturn(list);
        List<FileResponse> fileList = repository.getFileList("/");
        Assertions.assertNotNull(fileList);
    }

    @Test
    void writeNotSupportTest() {
        Assertions.assertThrows(OperationNotSupportedException.class,
                () -> repository.writeFile("/", "content"));
    }

    @Test
    void deleteNotSupportTest() {
        Assertions.assertThrows(OperationNotSupportedException.class,
                () -> repository.deleteFile("/"));
    }

    @Test
    @SneakyThrows
    void readFileTest() {
        List<String> list = new ArrayList<>();
        Mockito.when(jGitService.getFileContent(any(), any())).thenReturn("");
        String fileContent = repository.readFile("/");
        Assertions.assertNotNull(fileContent);
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
