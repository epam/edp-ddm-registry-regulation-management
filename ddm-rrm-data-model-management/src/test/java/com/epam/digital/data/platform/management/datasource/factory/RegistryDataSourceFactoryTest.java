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

package com.epam.digital.data.platform.management.datasource.factory;

import com.epam.digital.data.platform.management.config.DataSourceConfigurationProperties;
import com.epam.digital.data.platform.management.datasource.RegistryDataSource;
import net.bytebuddy.utility.RandomString;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    RegistryDataSourceFactory.class,
    DataSourceConfigurationProperties.class
})
@EnableConfigurationProperties
@TestPropertySource(properties = {
    "registry-regulation-management.data-source.driver-class-name = org.postgresql.Driver",
    "registry-regulation-management.data-source.base-jdbc-url = jdbc:postgresql://localhost:5432",
    "registry-regulation-management.data-source.username = postgres",
    "registry-regulation-management.data-source.password = password",
    "registry-regulation-management.data-source.registry-schema = registry"
})
@DisplayName("Registry data source factory test")
class RegistryDataSourceFactoryTest {

  @Autowired
  RegistryDataSourceFactory factory;

  @Test
  @DisplayName("created bean should have type RegistryDataSource")
  void createBeanTest() {
    var versionId = "196";

    var actualDs = factory.createBean(versionId);
    Assertions.assertThat(actualDs)
        .isInstanceOf(RegistryDataSource.class)
        .hasFieldOrPropertyWithValue("jdbcUrl", "jdbc:postgresql://localhost:5432/registry")
        .hasFieldOrPropertyWithValue("username", "postgres")
        .hasFieldOrPropertyWithValue("password", "password")
        .hasFieldOrPropertyWithValue("driverClassName", "org.postgresql.Driver");
  }

  @Test
  @DisplayName("bean should not be recreated")
  void recreateTest() {
    var versionId = RandomString.make();
    Assertions.assertThat(factory.shouldBeRecreated(versionId)).isFalse();
  }
}
