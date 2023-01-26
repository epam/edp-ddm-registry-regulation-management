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

package com.epam.digital.data.platform.management.core.context;


import com.epam.digital.data.platform.management.core.exception.VersionComponentCreationException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

/**
 * Class that is used for storing and accessing version based components.
 * <p>
 * Provides methods for selecting the component by version id and class type and destroying the
 * specific version context.
 * <p>
 * For accessing version based component it must have a corresponding
 * {@link VersionComponentFactory} stored in a Spring context.
 *
 * @see VersionComponentFactory
 */
@Slf4j
@RequiredArgsConstructor
public class VersionContextComponentManager {

  private final ConcurrentMap<String, VersionContext> contextMap = new ConcurrentHashMap<>();

  private final Map<Class<?>, VersionComponentFactory<?>> componentFactories;

  /**
   * Method that is used for selecting version based component by version id and component type.
   * <p>
   * Searches the requested component in inner storage and if it's not found tries to create the
   * component using corresponding {@link VersionComponentFactory}.
   *
   * @param versionId     version id
   * @param componentType class of the requested component type
   * @param <T>           type of the requested component
   * @return the version based component
   *
   * @throws IllegalArgumentException          if there is no registered
   *                                           {@link VersionComponentFactory} for
   *                                           {@code componentType}
   * @throws VersionComponentCreationException in case of any error during component creation
   */
  @NonNull
  public <T> T getComponent(@NonNull String versionId, @NonNull Class<T> componentType) {
    log.trace("Getting component with type '{}' for version '{}'", componentType, versionId);
    var componentFactory = componentFactories.get(componentType);
    if (Objects.isNull(componentFactory)) {
      throw new IllegalArgumentException(
          String.format("No VersionBeanFactory is registered for component type %s",
              componentType));
    }
    var context = contextMap.computeIfAbsent(versionId, VersionContext::new);
    return context.getComponent(componentType, componentFactory);
  }

  /**
   * Deletes all stored components that are corresponded to specified version
   *
   * @param versionId id of the version which context must be deleted
   */
  public void destroyContext(@NonNull String versionId) {
    contextMap.remove(versionId);
  }

  @Slf4j
  @RequiredArgsConstructor
  private static final class VersionContext {

    final String versionId;

    final ConcurrentMap<Class<?>, Object> componentMap = new ConcurrentHashMap<>();

    <T> T getComponent(Class<T> componentType, VersionComponentFactory<?> versionComponentFactory) {
      log.trace("Checking if component with type '{}' should be recreated for version '{}'",
          componentType, versionId);
      if (versionComponentFactory.shouldBeRecreated(versionId)) {
        log.trace("Component '{}' should be recreated for version '{}'. Remove current component",
            componentType, versionId);
        componentMap.remove(componentType);
      }

      log.trace("Selecting component '{}' for version '{}' or creating new one if not exist",
          componentType, versionId);
      var component = componentMap.computeIfAbsent(componentType,
          type -> versionComponentFactory.createComponent(versionId));

      return componentType.cast(component);
    }
  }
}
