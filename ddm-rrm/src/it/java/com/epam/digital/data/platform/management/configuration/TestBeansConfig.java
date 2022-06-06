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

package com.epam.digital.data.platform.management.configuration;

import com.epam.digital.data.platform.management.RegistryRegulationManagementApplication;
import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.forms.service.FormService;
import com.epam.digital.data.platform.management.groups.service.GroupService;
import com.epam.digital.data.platform.management.groups.validation.BpGroupingValidator;
import com.epam.digital.data.platform.management.osintegration.service.OpenShiftService;
import com.epam.digital.data.platform.management.restapi.mapper.ControllerMapper;
import com.epam.digital.data.platform.management.restapi.service.BuildStatusService;
import com.epam.digital.data.platform.management.service.BusinessProcessService;
import com.epam.digital.data.platform.management.service.DataModelFileManagementService;
import com.epam.digital.data.platform.management.service.ReadDataBaseTablesService;
import com.epam.digital.data.platform.management.settings.service.SettingService;
import com.epam.digital.data.platform.management.users.service.UserImportService;
import com.epam.digital.data.platform.management.versionmanagement.service.VersionManagementService;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@TestConfiguration
@ConditionalOnMissingBean(RegistryRegulationManagementApplication.class)
public class TestBeansConfig {

  @Bean
  public BusinessProcessService testBusinessProcessService() {
    return Mockito.mock(BusinessProcessService.class);
  }
  @Bean
  public GroupService testGroupService() {
    return Mockito.mock(GroupService.class);
  }
  @Bean
  public VersionManagementService testVersionManagementService() {
    return Mockito.mock(VersionManagementService.class);
  }
  @Bean
  public ControllerMapper testControllerMapper() {
    return Mockito.mock(ControllerMapper.class);
  }
  @Bean
  public DataModelFileManagementService testDataModelFileManagementService() {
    return Mockito.mock(DataModelFileManagementService.class);
  }
  @Bean
  public FormService testFormService() {
    return Mockito.mock(FormService.class);
  }
  @Bean
  public BpGroupingValidator testBpGroupingValidator() {
    return Mockito.mock(BpGroupingValidator.class);
  }
  @Bean
  public SettingService testSettingService() {
    return Mockito.mock(SettingService.class);
  }
  @Bean
  public ReadDataBaseTablesService testReadDataBaseTablesService() {
    return Mockito.mock(ReadDataBaseTablesService.class);
  }
  @Bean
  public BuildStatusService testBuildStatusService() {
    return Mockito.mock(BuildStatusService.class);
  }
  @Bean
  public GerritPropertiesConfig testGerritPropertiesConfig() {
    return Mockito.mock(GerritPropertiesConfig.class);
  }
  @Bean
  public UserImportService testUserImportService() {
    return Mockito.mock(UserImportService.class);
  }
  @Bean
  public OpenShiftService testOpenShiftService() {
    return Mockito.mock(OpenShiftService.class);
  }

}
