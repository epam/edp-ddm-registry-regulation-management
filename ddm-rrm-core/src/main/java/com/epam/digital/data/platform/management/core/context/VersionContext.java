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


import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;

/**
 * Class that is used for storing and accessing version based bean objects.
 * <p>
 * Provides methods for selecting the bean by version id and class type and destroying the specific
 * version context.
 * <p>
 * For accessing version based bean it must have a corresponding {@link VersionBeanFactory} stored
 * in a Spring context.
 *
 * @see VersionBeanFactory
 */
@RequiredArgsConstructor
public class VersionContext {

  private final ConcurrentMap<String, Context> contextMap = new ConcurrentHashMap<>();

  private final Map<Class<?>, VersionBeanFactory<?>> beanFactories;

  /**
   * Method that is used for selecting version based bean by version id and bean type.
   * <p>
   * Searches the requested bean in inner storage and if it's not found tries to create the bean
   * using corresponding {@link VersionBeanFactory}.
   *
   * @param versionId version id
   * @param beanType  class of the requested bean type
   * @param <T>       type of the requested bean
   * @return the version based bean
   *
   * @throws IllegalArgumentException if there is no registered {@link VersionBeanFactory} for
   *                                  {@code beanType}
   */
  @NonNull
  public <T> T getBean(@NonNull String versionId, @NonNull Class<T> beanType) {
    var beanFactory = beanFactories.get(beanType);
    if (Objects.isNull(beanFactory)) {
      throw new IllegalArgumentException(
          String.format("No VersionBeanFactory is registered for bean type %s", beanType));
    }
    var context = contextMap.computeIfAbsent(versionId, Context::new);
    return context.getBean(beanType, beanFactory);
  }

  /**
   * Deletes all stored beans that are corresponded to specified version
   *
   * @param versionId id of the version which context must be deleted
   */
  public void destroyContext(@NonNull String versionId) {
    contextMap.remove(versionId);
  }

  @RequiredArgsConstructor
  private static final class Context {

    final String versionId;

    final ConcurrentMap<Class<?>, Object> beanMap = new ConcurrentHashMap<>();

    <T> T getBean(Class<T> beanType, VersionBeanFactory<?> versionBeanFactory) {
      if (versionBeanFactory.shouldBeRecreated(versionId)) {
        beanMap.remove(beanType);
      }

      var bean = beanMap.computeIfAbsent(beanType,
          type -> versionBeanFactory.createBean(versionId));

      return beanType.cast(bean);
    }
  }
}
