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

package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.exception.GetProcessingException;
import com.epam.digital.data.platform.management.exception.OpenShiftInvocationException;
import com.epam.digital.data.platform.management.model.SecurityContext;
import com.epam.digital.data.platform.management.model.dto.CephFileInfoDto;
import com.epam.digital.data.platform.management.service.impl.OpenShiftServiceImpl;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobListBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

@EnableKubernetesMockClient
@ExtendWith(MockitoExtension.class)
class OpenShiftServiceTest {

  static final String NAMESPACE = "test";
  static final String JOB_NAME = "example";
  OpenShiftService openShiftService;
  KubernetesMockServer server;
  KubernetesClient client;

  @Mock
  UserImportService userImportService;

  @BeforeEach
  void init() {
    this.openShiftService = new OpenShiftServiceImpl(JOB_NAME, userImportService, server.createClient().getConfiguration());
  }

  @Test
  void validStartImportJob() {
    var id = UUID.randomUUID().toString();
    var cephEntityReadDto = new CephFileInfoDto(id, "test.txt", 1L);

    Job exampleJob = createJobBuilder().build();
    server.expect()
        .withPath("/apis/batch/v1/namespaces/test/jobs")
        .andReturn(HttpURLConnection.HTTP_OK, new JobListBuilder().addNewItemLike(exampleJob).and().build())
        .always();
    server.expect()
        .withPath("/apis/batch/v1/namespaces/test/jobs")
        .andReturn(HttpURLConnection.HTTP_OK, exampleJob)
        .once();
    when(userImportService.getFileInfo(any())).thenReturn(cephEntityReadDto);

    openShiftService.startImport(securityContext());

    verify(userImportService).getFileInfo(any());
    Job job = client.batch().v1().jobs().inNamespace(NAMESPACE).list().getItems().get(0);
    assertEquals(JOB_NAME, job.getMetadata().getName());
  }

  @Test
  void shouldThrowGetProcessingExceptionDueToEmptyCeph() {
    when(userImportService.getFileInfo(any())).thenReturn(new CephFileInfoDto());

    var exception = assertThrows(GetProcessingException.class,
        () -> openShiftService.startImport(securityContext()));

    assertThat(exception.getMessage()).isEqualTo("Bucket is empty, nothing to import");
  }

  @Test
  void shouldConvertAnyOpenShiftExceptionToOpenShiftInvocationException() {
    var cephEntityReadDto = new CephFileInfoDto(UUID.randomUUID().toString(), "test.txt", 1L);
    when(userImportService.getFileInfo(any())).thenReturn(cephEntityReadDto);
    var exception = assertThrows(OpenShiftInvocationException.class,
        () -> openShiftService.startImport(securityContext()));

    assertThat(exception.getMessage()).isEqualTo("Unable to create Job");
  }

  private JobBuilder createJobBuilder() {
    return new JobBuilder()
        .withApiVersion("batch/v1")
        .withNewMetadata()
        .withName("example")
        .withUid("3Dc4c8746c-94fd-47a7-ac01-11047c0323b4")
        .withLabels(Collections.singletonMap("name", "example"))
        .withAnnotations(Collections.singletonMap("annotation1", "some-very-long-annotation"))
        .endMetadata()
        .withNewSpec()
        .withNewTemplate()
        .withNewSpec()
        .addNewContainer()
        .withName("pi")
        .withImage("perl")
        .withArgs("perl", "-Mbignum=bpi", "-wle", "print bpi(2000)")
        .endContainer()
        .withRestartPolicy("Never")
        .endSpec()
        .endTemplate()
        .endSpec();
  }

  private SecurityContext securityContext() {
    var context = new SecurityContext();
    context.setAccessToken("stub");
    return context;
  }
}