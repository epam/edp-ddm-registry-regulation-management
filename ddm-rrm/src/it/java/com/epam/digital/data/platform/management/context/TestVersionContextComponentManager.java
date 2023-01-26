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

package com.epam.digital.data.platform.management.context;

import com.epam.digital.data.platform.management.core.context.VersionContextComponentManager;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Used for modifying existing {@link VersionContextComponentManager}
 */
@Component
@RequiredArgsConstructor
public class TestVersionContextComponentManager {

  private final VersionContextComponentManager versionContextComponentManager;

  /**
   * Puts or replaces existing component with new one
   *
   * @param versionCandidateId id of version to replace component for
   * @param type               class object of the type of the component
   * @param component          the new component to be set
   * @param <T>                type of the component
   */
  @SuppressWarnings("unchecked")
  public <T> void setComponent(String versionCandidateId, Class<T> type, T component) {
    versionContextComponentManager.getComponent(versionCandidateId, type);

    var contextMap = (Map<String, Object>) ReflectionTestUtils.getField(
        versionContextComponentManager,
        "contextMap");
    var context = contextMap.get(versionCandidateId);
    var componentMap = (Map<Class<T>, Object>) ReflectionTestUtils.getField(context,
        "componentMap");
    componentMap.put(type, component);
  }
}
