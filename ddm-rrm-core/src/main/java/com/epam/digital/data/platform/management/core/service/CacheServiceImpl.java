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

import com.epam.digital.data.platform.management.core.utils.ETagUtils;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

  private static final String CONFLICTS_CACHE_NAME = "conflicts";
  private static final String LATEST_REBASE_CACHE_NAME = "latestRebase";
  private static final String ETAG_CACHE_NAME = "etagCache";

  private final CacheManager cacheManager;

  @Override
  public List<String> getConflictsCache(String cacheKey) {
    Cache.ValueWrapper valueWrapper = cacheManager.getCache(CONFLICTS_CACHE_NAME).get(cacheKey);
    if (Objects.nonNull(valueWrapper)) {
      return (List<String>) valueWrapper.get();
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public void updateConflictsCache(String cacheKey, List<String> conflicts) {
    Cache conflictCache = cacheManager.getCache(CONFLICTS_CACHE_NAME);
    conflictCache.evictIfPresent(cacheKey);
    if (Objects.nonNull(conflicts)) {
      conflictCache.put(cacheKey, conflicts);
    }
  }

  @Override
  public LocalDateTime getLatestRebaseCache(String cacheKey) {
    Cache.ValueWrapper valueWrapper = cacheManager.getCache(LATEST_REBASE_CACHE_NAME).get(cacheKey);
    if (Objects.nonNull(valueWrapper)) {
      return (LocalDateTime) valueWrapper.get();
    } else {
      return null;
    }
  }

  @Override
  public void updateLatestRebaseCache(String cacheKey, LocalDateTime latestRebase) {
    Cache conflictCache = cacheManager.getCache(LATEST_REBASE_CACHE_NAME);
    conflictCache.evictIfPresent(cacheKey);
    conflictCache.put(cacheKey, latestRebase);
  }

  @Override
  @Cacheable(cacheNames = ETAG_CACHE_NAME, key = "#versionId + #fileName")
  public String getEtag(String versionId, String fileName, String fileContent) {
    return ETagUtils.getETagFromContent(fileContent);
  }

  @Override
  @CacheEvict(cacheNames = ETAG_CACHE_NAME, key = "#versionId + #fileName")
  public void evictEtag(String versionId, String fileName) {
  }
}
