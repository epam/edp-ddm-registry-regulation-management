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
import com.epam.digital.data.platform.management.core.context.VersionBeanFactory;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Abstract factory that is used for creating version based data-sources.
 *
 * @param <T> type that extends {@link DataSource}
 */
@Component
@RequiredArgsConstructor
public abstract class AbstractDataSourceFactory<T extends DataSource> implements
    VersionBeanFactory<T> {

  protected final DataSourceConfigurationProperties dsProps;

  @Override
  @NonNull
  public T createBean(@NonNull String versionId) {
    return DataSourceBuilder.create()
        .type(getBeanType())
        .driverClassName(dsProps.getDriverClassName())
        .url(String.format("%s/%s", dsProps.getBaseJdbcUrl(), getSchema()))
        .username(dsProps.getUsername())
        .password(dsProps.getPassword())
        .build();
  }

  /**
   * @return database schema name to connect to
   */
  protected abstract String getSchema();
}
