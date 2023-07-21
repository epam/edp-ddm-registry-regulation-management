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

package com.epam.digital.data.platform.management.core.service;

import com.epam.digital.data.platform.management.core.service.EtagCacheServiceImplTest.CachingTestConfig;
import com.epam.digital.data.platform.management.core.utils.ETagUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {CacheServiceImpl.class, CachingTestConfig.class})
class EtagCacheServiceImplTest {

  @Autowired
  CacheService cacheService;
  @Autowired
  CacheManager cacheManager;

  @Test
  void etagCacheTest() {
    var version = "124";
    var fileName = "file";
    var fileContent = "content";

    var result = cacheService.getEtag(version, fileName, fileContent);

    Assertions.assertThat(result)
        .isEqualTo(ETagUtils.getETagFromContent(fileContent));
    Assertions.assertThat(cacheManager.getCache("etagCache").get(version + fileName).get())
        .isEqualTo(ETagUtils.getETagFromContent(fileContent));

    cacheService.evictEtag(version, fileName);

    Assertions.assertThat(cacheManager.getCache("etagCache").get(version + fileName))
        .isNull();
  }

  @EnableCaching
  @Configuration
  public static class CachingTestConfig {

    @Bean
    public CacheManager cacheManager() {
      return new ConcurrentMapCacheManager("etagCache");
    }
  }
}
