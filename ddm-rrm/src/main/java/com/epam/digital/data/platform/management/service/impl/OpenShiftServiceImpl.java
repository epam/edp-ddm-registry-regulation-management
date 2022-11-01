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

package com.epam.digital.data.platform.management.service.impl;

import com.epam.digital.data.platform.management.exception.GetProcessingException;
import com.epam.digital.data.platform.management.exception.OpenShiftInvocationException;
import com.epam.digital.data.platform.management.model.SecurityContext;
import com.epam.digital.data.platform.management.service.OpenShiftService;
import com.epam.digital.data.platform.management.service.UserImportService;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;

@Service
public class OpenShiftServiceImpl implements OpenShiftService {

  private static final String CALL_APP_JAR = ">- \njava -jar app.jar --id='%s' --USER_ACCESS_TOKEN='%s' --REQUEST_ID='%s'";
  private static final String MDC_TRACE_ID_HEADER = "X-B3-TraceId";
  private static final String JOB_NAME_LABEL = "name";
  private static final String MINUS_DELIMITER = "-";

  private final UserImportService userImportService;
  private final Config openShiftConfig;
  private final String jobName;

  public OpenShiftServiceImpl(@Value("${openshift.job.name}") String jobName,
      UserImportService userImportService,
      Config openShiftConfig) {
    this.userImportService = userImportService;
    this.openShiftConfig = openShiftConfig;
    this.jobName = jobName;
  }

  @Override
  public void startImport(SecurityContext securityContext) {
    var fileInfo = userImportService.getFileInfo(securityContext);
    if (StringUtils.isBlank(fileInfo.getId())) {
      throw new GetProcessingException("Bucket is empty, nothing to import");
    }

    try (OpenShiftClient openShiftClient = new DefaultOpenShiftClient(openShiftConfig)) {

      var job = getJob(openShiftClient);

      var clonedJob = cloneJob(job, fileInfo.getId(), securityContext);

      openShiftClient.batch().v1().jobs().createOrReplace(clonedJob);
    } catch (KubernetesClientException e) {
      throw new OpenShiftInvocationException("Unable to create Job", e);
    }
  }

  private Job cloneJob(Job job, String fileId, SecurityContext securityContext) {
    JobSpec spec = job.getSpec();
    var container = spec
        .getTemplate()
        .getSpec()
        .getContainers()
        .stream()
        .findFirst()
        .orElseThrow(() -> new OpenShiftInvocationException("Missed or broken container job part for job: " + jobName));

    var callAppJar = String.format(CALL_APP_JAR, fileId, securityContext.getAccessToken(), MDC.get(MDC_TRACE_ID_HEADER));
    var jobCommand = Arrays.asList("sh", "-c", callAppJar);

    container.setCommand(jobCommand);
    spec.setManualSelector(true);

    var metadata = job.getMetadata();
    metadata.setResourceVersion(null);
    metadata.setName(StringUtils.joinWith(MINUS_DELIMITER, job.getMetadata().getName(), UUID.randomUUID()));

    return job;
  }

  private Job getJob(OpenShiftClient openShiftClient) {
    return openShiftClient
        .batch()
        .v1()
        .jobs()
        .list()
        .getItems()
        .stream()
        .filter(job -> StringUtils.equals(jobName, job.getMetadata().getLabels().get(JOB_NAME_LABEL)))
        .findFirst()
        .orElseThrow(() -> new OpenShiftInvocationException("Missed k8s job with name: " + jobName));
  }
}