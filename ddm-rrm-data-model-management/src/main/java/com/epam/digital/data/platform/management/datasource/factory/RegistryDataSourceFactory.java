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
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Factory that is used for creating version based {@link RegistryDataSource}
 */
@Component
public class RegistryDataSourceFactory extends AbstractDataSourceFactory<RegistryDataSource> {

  public RegistryDataSourceFactory(DataSourceConfigurationProperties dsProps) {
    super(dsProps);
  }

  @Override
  protected String getSchema() {
    return dsProps.getRegistrySchema();
  }

  @Override
  @NonNull
  public Class<RegistryDataSource> getBeanType() {
    return RegistryDataSource.class;
  }
}
