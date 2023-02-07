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

package com.epam.digital.data.platform.management.factory;

import com.epam.digital.data.platform.management.config.DataSourceConfigurationProperties;
import com.epam.digital.data.platform.management.core.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.core.context.VersionComponentFactory;
import com.epam.digital.data.platform.management.datasource.RegistryDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Factory that is used for creating version based {@link RegistryDataSource}
 */
@Component
@RequiredArgsConstructor
public class RegistryDataSourceFactory implements VersionComponentFactory<RegistryDataSource> {

  private final DataSourceConfigurationProperties dsProps;
  private final GerritPropertiesConfig gerritPropertiesConfig;

  @Override
  @NonNull
  public RegistryDataSource createComponent(@NonNull String versionId) {
    return DataSourceBuilder.create()
        .type(getComponentType())
        .driverClassName(dsProps.getDriverClassName())
        .url(String.format("%s/%s", dsProps.getBaseJdbcUrl(), getDataBaseName(versionId)))
        .username(dsProps.getUsername())
        .password(dsProps.getPassword())
        .build();
  }

  @Override
  @NonNull
  public Class<RegistryDataSource> getComponentType() {
    return RegistryDataSource.class;
  }

  private String getDataBaseName(@NonNull String versionId) {
    return gerritPropertiesConfig.getHeadBranch().equals(versionId)
        ? dsProps.getRegistryDataBase()
        : dsProps.getRegistryDevDataBasePrefix().concat(versionId);
  }
}
