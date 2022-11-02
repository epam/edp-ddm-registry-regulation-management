/*
 * Copyright 2022 EPAM Systems.
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

package com.epam.digital.data.platform.management;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.http.MediaType;

@DisplayName("Forms in master version controller tests")
class MasterVersionFormsControllerIT extends BaseIT {

  @Autowired
  private CacheManager cacheManager;
  private static final String DATE_CACHE_NAME = "dates";

  @Nested
  @DisplayName("GET /versions/master/forms/{formName}")
  class MasterVersionFormsGetFormByNameControllerIT {

    @Test
    @DisplayName("should return 200 with form content")
    @SneakyThrows
    void getForm() {
      // add file to "remote" repo
      final var expectedFormContent = context.getResourceContent(
          "/versions/master/forms/{formName}/GET/john-does-form.json");
      context.addFileToHeadRepo("/forms/john-does-form.json", expectedFormContent);

      // perform query
      mockMvc.perform(
          get("/versions/master/forms/{formName}", "john-does-form")
              .accept(MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON),
          content().json(expectedFormContent)
      );
    }

    @Test
    @DisplayName("should return 404 if form doesn't exist")
    @SneakyThrows
    void getForm_formDoesNotExist() {
      mockMvc.perform(
          get("/versions/master/forms/{formName}", "john-does-form")
              .accept(MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isNotFound(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.code", is("FORM_NOT_FOUND_EXCEPTION")),
          jsonPath("$.details", is("Form john-does-form not found"))
      );
    }
  }

  @Nested
  @DisplayName("GET /versions/master/forms")
  class CandidateVersionFormsGetFormListControllerIT {

    @Test
    @DisplayName("should return 200 with all found forms")
    @SneakyThrows
    void getFormsInMaster() {
      // add files to "remote" repo
      final var johnDoesFormContent = context.getResourceContent(
          "/versions/master/forms/GET/john-does-form.json");
      context.addFileToHeadRepo("/forms/john-does-form.json", johnDoesFormContent);
      final var mrSmithsFormContent = context.getResourceContent(
          "/versions/master/forms/GET/mr-smiths-form.json");
      context.addFileToHeadRepo("/forms/mr-smiths-form.json", mrSmithsFormContent);

      // define expected john-does-form dates
      final var expectedJohnDoesFormDates = context.getHeadRepoDatesByPath(
          "forms/john-does-form.json");

      // perform query
      mockMvc.perform(
          get("/versions/master/forms")
              .accept(MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$[0].name", is("john-does-form")),
          jsonPath("$[0].title", is("John Doe's form")),
          jsonPath("$[0].created", is(expectedJohnDoesFormDates.getCreated())),
          jsonPath("$[0].updated", is(expectedJohnDoesFormDates.getUpdated())),
          jsonPath("$[1].name", is("mr-smiths-form")),
          jsonPath("$[1].title", is("Mr Smith's form")),
          jsonPath("$[1].created", is("2022-10-28T20:21:48.845Z")),
          jsonPath("$[1].updated", is("2022-10-28T20:56:32.309Z"))
      );

      final var datesCache = cacheManager.getCache(DATE_CACHE_NAME);
      Assertions.assertThat(datesCache).isNotNull();

      final var johnDoesCacheKey = new SimpleKey("head-branch", "forms/john-does-form.json");
      final var valueWrapper = datesCache.get(johnDoesCacheKey);
      Assertions.assertThat(valueWrapper).isNotNull();

      Thread.sleep(10000);
      final var emptyDatesCache = cacheManager.getCache(DATE_CACHE_NAME);
      Assertions.assertThat(emptyDatesCache)
          .isNotNull()
          .extracting(cache -> cache.get(johnDoesCacheKey))
          .isNull();
    }

    @Test
    @DisplayName("should return 200 with empty array if there are no forms")
    @SneakyThrows
    void getFormsInMaster_noForms() {
      mockMvc.perform(
          get("/versions/master/forms")
              .accept(MediaType.APPLICATION_JSON_VALUE)
      ).andExpectAll(
          status().isOk(),
          content().contentType("application/json"),
          jsonPath("$", hasSize(0))
      );
    }
  }
}
