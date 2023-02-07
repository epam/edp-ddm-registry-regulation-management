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

import com.epam.digital.data.platform.management.core.context.VersionComponentFactory;
import com.epam.digital.data.platform.management.core.context.VersionContextComponentManager;
import com.epam.digital.data.platform.management.core.exception.VersionComponentCreationException;
import com.epam.digital.data.platform.management.datasource.RegistryDataSource;
import java.sql.SQLException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import schemacrawler.schema.Catalog;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.utility.SchemaCrawlerUtility;

/**
 * Factory that is used for creating version based {@link Catalog}
 */
@Component
@RequiredArgsConstructor
public class SchemaCrawlerFactory implements VersionComponentFactory<Catalog> {

  @Lazy
  @Autowired
  private VersionContextComponentManager versionContextComponentManager;
  private final SchemaCrawlerOptions options;

  @Override
  @NonNull
  public Catalog createComponent(@NonNull String versionId) {
    var registryDs = versionContextComponentManager.getComponent(versionId, RegistryDataSource.class);

    try (var conn = registryDs.getConnection()) {
      return SchemaCrawlerUtility.getCatalog(conn, options);
    } catch (SchemaCrawlerException | SQLException e) {
      throw new VersionComponentCreationException(
          String.format("Schema crawler catalog couldn't be created: %s", e.getMessage()), e);
    }
  }

  /**
   * Should be recreated every time as it connects to database during creation
   *
   * @return true
   */
  @Override
  public boolean shouldBeRecreated(@NonNull String versionId) {
    return true;
  }

  @Override
  @NonNull
  public Class<Catalog> getComponentType() {
    return Catalog.class;
  }
}
