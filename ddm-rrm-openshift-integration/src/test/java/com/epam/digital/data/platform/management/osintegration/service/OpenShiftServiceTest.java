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

package com.epam.digital.data.platform.management.osintegration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.epam.digital.data.platform.management.osintegration.exception.GetProcessingException;
import com.epam.digital.data.platform.management.osintegration.exception.OpenShiftInvocationException;
import com.epam.digital.data.platform.management.security.model.SecurityContext;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobListBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@EnableKubernetesMockClient
@ExtendWith(SpringExtension.class)
class OpenShiftServiceTest {

  static final String NAMESPACE = "test";
  static final String JOB_NAME = "example";
  public static final String USER_ACCESS_TOKEN = "accessToken";
  OpenShiftService openShiftService;
  KubernetesMockServer server;
  KubernetesClient client;

  @BeforeEach
  void init() {
    this.openShiftService = new OpenShiftServiceImpl(server.createClient().getConfiguration(),
        JOB_NAME, USER_ACCESS_TOKEN);
  }

  @Test
  void validStartImportJob() {
    var id = UUID.randomUUID().toString();

    Job exampleJob = createJobBuilder().build();
    server.expect()
        .withPath("/apis/batch/v1/namespaces/test/jobs")
        .andReturn(HttpURLConnection.HTTP_OK,
            new JobListBuilder().addNewItemLike(exampleJob).and().build())
        .always();
    server.expect()
        .withPath("/apis/batch/v1/namespaces/test/jobs")
        .andReturn(HttpURLConnection.HTTP_OK, exampleJob)
        .once();

    server.expect()
        .withPath("/api/v1/namespaces/test/secrets/accessToken")
        .andReturn(HttpURLConnection.HTTP_OK, new SecretBuilder()
            .withNewMetadata()
            .withName(USER_ACCESS_TOKEN)
            .endMetadata()
            .withType("kubernetes.io/tls")
            .withImmutable(false)
            .addToStringData(new HashMap<>())
            .build()).always();

    openShiftService.startImport(id, securityContext());

    Job job = client.batch().v1().jobs().inNamespace(NAMESPACE).list().getItems().get(0);
    assertThat(job.getMetadata().getName()).isEqualTo(JOB_NAME);
  }

  @Test
  void shouldThrowGetProcessingExceptionDueToEmptyCeph() {
    assertThatCode(() -> openShiftService.startImport(null, securityContext()))
        .isInstanceOf(GetProcessingException.class)
        .hasMessage("Bucket is empty, nothing to import");
  }

  @Test
  void shouldConvertAnyOpenShiftExceptionToOpenShiftInvocationException() {
    assertThatCode(() -> openShiftService.startImport(UUID.randomUUID().toString(), securityContext()))
        .isInstanceOf(OpenShiftInvocationException.class)
        .hasMessage("Unable to create Job");
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