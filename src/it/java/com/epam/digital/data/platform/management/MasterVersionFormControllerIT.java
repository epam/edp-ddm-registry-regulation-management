package com.epam.digital.data.platform.management;

import static com.epam.digital.data.platform.management.config.CacheCustomizer.DATE_CACHE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.service.JGitService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

public class MasterVersionFormControllerIT extends BaseIT {

  @Autowired
  private CacheManager cacheManager;

  @Autowired
  private JGitService jGitService;

  @Test
  @SneakyThrows
  void testDatesInCache() {
    mockMvc.perform(MockMvcRequestBuilders.get("/versions/master/forms")
            .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpectAll(
            status().isOk(),
            content().contentType("application/json"),
            jsonPath("$.[0].name", is("someFile")),
            jsonPath("$.[0].title", is("title")),
            jsonPath("$.[0].created", is("1970-01-01T00:00:00.000Z")),
            jsonPath("$.[0].updated", is("1970-01-01T00:00:00.000Z")));
    Cache dates = cacheManager.getCache(DATE_CACHE_NAME);
    assertThat(dates).isNotNull();
    SimpleKey cacheKey = new SimpleKey("head-branch", "forms/someFile");
    ValueWrapper valueWrapper = dates.get(cacheKey);
    assertThat(valueWrapper).isNotNull();

    jGitService.formDatesCacheEvict();
    dates = cacheManager.getCache(DATE_CACHE_NAME);
    assertThat(dates.get(cacheKey)).isNull();
  }
}
