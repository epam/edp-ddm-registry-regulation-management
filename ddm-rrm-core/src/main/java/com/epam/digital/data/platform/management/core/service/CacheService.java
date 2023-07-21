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

package com.epam.digital.data.platform.management.core.service;

import java.time.LocalDateTime;
import java.util.List;

/** Provide methods to work with cache */
public interface CacheService {

  List<String> getConflictsCache(String cacheKey);

  void updateConflictsCache(String cacheKey, List<String> conflicts);

  LocalDateTime getLatestRebaseCache(String cacheKey);

  void updateLatestRebaseCache(String cacheKey, LocalDateTime latestRebase);
}
