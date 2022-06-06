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
package com.epam.digital.data.platform.management.config;

import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import schemacrawler.inclusionrule.InclusionRule;
import schemacrawler.inclusionrule.RegularExpressionInclusionRule;
import schemacrawler.schemacrawler.LimitOptionsBuilder;
import schemacrawler.schemacrawler.LoadOptionsBuilder;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder;
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder;

@Configuration
public class SchemaCrawlerConfiguration {

  @Bean
  public LimitOptionsBuilder limitOptionsBuilder(SchemaCrawlerProperties config) {
    return LimitOptionsBuilder.builder()
        .includeSchemas(new RegularExpressionInclusionRule(config.getSchema()))
        .includeTables(includeTables(config))
        .includeColumns(includeFields(config));
  }

  private InclusionRule includeTables(SchemaCrawlerProperties config) {
    return name -> !config.getExcludeTables().contains(name)
        && config.getExcludeTablePrefixes().stream()
        .noneMatch(prfx -> name.startsWith(config.getSchema() + "." + prfx))
        && config.getExcludeTableSuffixes().stream().noneMatch(name::endsWith);
  }

  private InclusionRule includeFields(SchemaCrawlerProperties config) {
    return fieldName -> Arrays.stream(fieldName.split("\\."))
        .reduce((acc, cleanFieldName) -> cleanFieldName)
        .map(cleanColumnName -> config.getExcludeFieldPrefixes().stream()
            .noneMatch(cleanColumnName::startsWith))
        .orElse(true);
  }

  @Bean
  public LoadOptionsBuilder loadOptionsBuilder() {
    return LoadOptionsBuilder.builder()
        .withSchemaInfoLevel(SchemaInfoLevelBuilder.standard());
  }

  @Bean
  public SchemaCrawlerOptions schemaCrawlerOptions(LoadOptionsBuilder loadOptionsBuilder,
      LimitOptionsBuilder limitOptionsBuilder) {
    return SchemaCrawlerOptionsBuilder.newSchemaCrawlerOptions()
        .withLimitOptions(limitOptionsBuilder.toOptions())
        .withLoadOptions(loadOptionsBuilder.toOptions());
  }
}
