/*
 * Copyright 2023 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.management.gitintegration.service;

import com.epam.digital.data.platform.management.gitintegration.model.FileDatesDto;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatesCacheService {

  public static final String DATE_CACHE_NAME = "dates";

  private final CacheManager cacheManager;

  public FileDatesDto getDates(String versionId, String filePath) {
    var cache = cacheManager.getCache(DATE_CACHE_NAME);
    Map<String, FileDatesDto> cachedMap = cache.get(versionId, Map.class);
    if (Objects.isNull(cachedMap)) {
      return null;
    }
    return cachedMap.get(filePath);
  }

  @SuppressWarnings("unchecked")
  public Map<String, FileDatesDto> getDatesCache(String versionId) {
    var cache = cacheManager.getCache(DATE_CACHE_NAME);
    var cachedMap = cache.get(versionId, Map.class);
    if (Objects.isNull(cachedMap)) {
      return new HashMap<>();
    }
    return new HashMap<String, FileDatesDto>(cachedMap);
  }

  public void setDatesCache(String versionId, Map<String, FileDatesDto> datesDtoMap) {
    var cache = cacheManager.getCache(DATE_CACHE_NAME);
    cache.evictIfPresent(versionId);
    cache.put(versionId, new HashMap<>(datesDtoMap));
  }

  @SuppressWarnings("unchecked")
  public void setDatesToCache(String versionId, String filePath, FileDatesDto fileDatesDto) {
    var cache = cacheManager.getCache(DATE_CACHE_NAME);
    Map<String, FileDatesDto> cachedMap = cache.get(versionId, Map.class);
    if (Objects.isNull(cachedMap)) {
      cachedMap = new HashMap<>();
      cache.put(versionId, cachedMap);
    }
    cachedMap.put(filePath, fileDatesDto);
  }

}
