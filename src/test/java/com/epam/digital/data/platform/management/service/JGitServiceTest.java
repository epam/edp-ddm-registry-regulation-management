/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.digital.data.platform.management.service;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
@ExtendWith(SpringExtension.class)
public class JGitServiceTest {
    @Mock
    private JGitService jGitService;
    @Test
    @SneakyThrows
    void testCloneRepository() {
        jGitService.cloneRepo(any());
        Mockito.verify(jGitService, times(1)).cloneRepo(any());
    }
    @Test
    @SneakyThrows
    void testPullRepository() {
        jGitService.pull(any());
        Mockito.verify(jGitService, times(1)).pull(any());
    }
    @Test
    @SneakyThrows
    void testGetFilesInPath() {
        jGitService.getFilesInPath(any(), any());
        Mockito.verify(jGitService, times(1)).getFilesInPath(any(), any());
    }
    @Test
    @SneakyThrows
    void testGetFileContent() {
        jGitService.getFileContent(any(), any());
        Mockito.verify(jGitService, times(1)).getFileContent(any(), any());
    }
    @Test
    @SneakyThrows
    void amendTest(){
        jGitService.amend(any(), any());
        Mockito.verify(jGitService, times(1)).amend(any(), any());
    }
    @Test
    @SneakyThrows
    void deleteTest(){
        jGitService.delete(any(), any());
        Mockito.verify(jGitService, times(1)).delete(any(), any());
    }
}
