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

import java.util.Random;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class RecreateTestVersionBeanFactory implements VersionBeanFactory<Integer> {

  @Override
  @NonNull
  public Integer createBean(@NonNull String versionId) {
    return new Random().nextInt();
  }

  @Override
  public boolean shouldBeRecreated(@NonNull String versionId) {
    return true;
  }

  @Override
  @NonNull
  public Class<Integer> getBeanType() {
    return Integer.class;
  }
}
