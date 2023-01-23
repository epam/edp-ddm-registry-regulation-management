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

import org.springframework.lang.NonNull;

/**
 * Version bean factory that is used for creating version scoped beans by {@link VersionContext}
 *
 * @param <T> type of the bean to be created by this factory
 * @see VersionContext
 */
public interface VersionBeanFactory<T> {

  /**
   * Creates the instance of a bean
   *
   * @param versionId id of a version for which it's needed to create a bean
   * @return bean instance
   */
  @NonNull
  T createBean(@NonNull String versionId);

  /**
   * @return true if bean should be recreated for this versionId and false otherwise
   */
  default boolean shouldBeRecreated(@NonNull String versionId) {
    return false;
  }

  /**
   * @return class object that represents a bean type
   */
  @NonNull
  Class<T> getBeanType();
}
