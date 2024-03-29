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

package com.epam.digital.data.platform.management.core.config;

import com.epam.digital.data.platform.management.core.context.VersionComponentFactory;
import com.epam.digital.data.platform.management.core.context.VersionContextComponentManager;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class that is used for creating and registering
 * {@link VersionContextComponentManager} bean
 */
@RequiredArgsConstructor
@Configuration
public class VersionContextConfig {

  private final Collection<VersionComponentFactory<?>> versionBeanFactories;

  @Bean
  public VersionContextComponentManager versionContext() {
    Map<Class<?>, VersionComponentFactory<?>> versionBeanFactoryMap = versionBeanFactories.stream()
        .collect(Collectors.toMap(VersionComponentFactory::getComponentType, Function.identity()));

    return new VersionContextComponentManager(versionBeanFactoryMap);
  }
}
