package com.epam.digital.data.platform.management.config;


import java.util.List;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.stereotype.Component;

@Component
@EnableCaching
public class CacheCustomizer implements CacheManagerCustomizer<ConcurrentMapCacheManager> {

  public static final String DATE_CACHE_NAME = "dates";

  @Override
  public void customize(ConcurrentMapCacheManager cacheManager) {
    cacheManager.setCacheNames(List.of(DATE_CACHE_NAME));
  }
}
