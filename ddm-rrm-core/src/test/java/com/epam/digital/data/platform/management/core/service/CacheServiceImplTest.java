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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import schemacrawler.schema.Catalog;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {CacheServiceImpl.class})
class CacheServiceImplTest {

  private static final String CONFLICTS_CACHE_NAME = "conflicts";
  private static final String LATEST_REBASE_CACHE_NAME = "latestRebase";
  private static final String CATALOG_CACHE_NAME = "catalog";
  private static final String CACHE_KEY = "key";

  @Autowired CacheServiceImpl cacheService;
  @MockBean CacheManager cacheManager;
  @Mock Cache cache;
  @Mock Cache.ValueWrapper valueWrapper;

  @Test
  void getConflictsCacheTest() {
    when(cacheManager.getCache(CONFLICTS_CACHE_NAME)).thenReturn(cache);
    when(cache.get(CACHE_KEY)).thenReturn(valueWrapper);
    List<String> fileNames = List.of("fileName1", "fileName2");
    when(valueWrapper.get()).thenReturn(fileNames);

    var result = cacheService.getConflictsCache(CACHE_KEY);

    assertThat(result).isEqualTo(fileNames);
  }

  @Test
  void getConflictsCacheTest_cacheNull() {
    when(cacheManager.getCache(CONFLICTS_CACHE_NAME)).thenReturn(cache);
    when(cache.get(CACHE_KEY)).thenReturn(null);

    var result = cacheService.getConflictsCache(CACHE_KEY);

    assertThat(result).isEqualTo(Collections.emptyList());
  }

  @Test
  void updateConflictsCache() {
    when(cacheManager.getCache(CONFLICTS_CACHE_NAME)).thenReturn(cache);
    List<String> conflicts = Collections.emptyList();

    cacheService.updateConflictsCache(CACHE_KEY, conflicts);

    verify(cache).evictIfPresent(CACHE_KEY);
    verify(cache).put(CACHE_KEY, conflicts);
  }

  @Test
  void getLatestRebaseCacheTest() {
    when(cacheManager.getCache(LATEST_REBASE_CACHE_NAME)).thenReturn(cache);
    when(cache.get(CACHE_KEY)).thenReturn(valueWrapper);
    LocalDateTime latestRebase = LocalDateTime.now();
    when(valueWrapper.get()).thenReturn(latestRebase);

    var result = cacheService.getLatestRebaseCache(CACHE_KEY);

    assertThat(result).isEqualTo(latestRebase);
  }

  @Test
  void getLatestRebaseCacheTest_cacheNull() {
    when(cacheManager.getCache(LATEST_REBASE_CACHE_NAME)).thenReturn(cache);
    when(cache.get(CACHE_KEY)).thenReturn(null);

    var result = cacheService.getLatestRebaseCache(CACHE_KEY);

    assertThat(result).isNull();
  }

  @Test
  void updateLatestRebaseCache() {
    when(cacheManager.getCache(LATEST_REBASE_CACHE_NAME)).thenReturn(cache);
    LocalDateTime latestRebase = LocalDateTime.now();

    cacheService.updateLatestRebaseCache(CACHE_KEY, latestRebase);

    verify(cache).evictIfPresent(CACHE_KEY);
    verify(cache).put(CACHE_KEY, latestRebase);
  }

  @Test
  void getCatalogCache() {
    when(cacheManager.getCache(CATALOG_CACHE_NAME)).thenReturn(cache);
    when(cache.get(CACHE_KEY)).thenReturn(valueWrapper);
    Catalog expected = mock(Catalog.class);
    when(valueWrapper.get()).thenReturn(expected);

    Catalog result = cacheService.getCatalogCache(CACHE_KEY);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getCatalogCache_cacheNull() {
    when(cacheManager.getCache(CATALOG_CACHE_NAME)).thenReturn(cache);
    when(cache.get(CACHE_KEY)).thenReturn(null);

    Catalog result = cacheService.getCatalogCache(CACHE_KEY);

    assertThat(result).isNull();
  }

  @Test
  void updateCatalogCache() {
    when(cacheManager.getCache(CATALOG_CACHE_NAME)).thenReturn(cache);
    Catalog catalog = mock(Catalog.class);

    cacheService.updateCatalogCache(CACHE_KEY, catalog);

    verify(cache).evictIfPresent(CACHE_KEY);
    verify(cache).put(CACHE_KEY, catalog);
  }

  @Test
  void clearCatalogCache() {
    when(cacheManager.getCache(CATALOG_CACHE_NAME)).thenReturn(cache);

    cacheService.clearCatalogCache(CACHE_KEY);

    verify(cache).evictIfPresent(CACHE_KEY);
  }
}
