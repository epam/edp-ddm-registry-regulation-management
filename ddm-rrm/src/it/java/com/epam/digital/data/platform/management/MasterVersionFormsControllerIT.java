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

package com.epam.digital.data.platform.management;

import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.core.config.JacksonConfig;
import com.google.gson.JsonParser;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
      // add file to "remote" repo and pull head repo
      final var expectedFormContent = context.getResourceContent(
          "/versions/master/forms/{formName}/GET/john-does-form.json");
      context.addFileToRemoteHeadRepo("/forms/john-does-form.json", expectedFormContent);
      context.pullHeadRepo();

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
    @DisplayName("should return 404 if form hasn't been pulled")
    @SneakyThrows
    void getForm_formHasNotBeenPulled() {
      // add file to "remote" repo and DO NOT pull the head repo
      final var expectedFormContent = context.getResourceContent(
          "/versions/master/forms/{formName}/GET/john-does-form.json");
      context.addFileToRemoteHeadRepo("/forms/john-does-form.json", expectedFormContent);

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
  class MasterVersionFormsGetFormListControllerIT {

    @Test
    @DisplayName("should return 200 with all pulled forms")
    @SneakyThrows
    void getFormsInMaster() {
      // add 2 files to "remote" repo pull head branch repo and add 1 more file to "remote"
      final var johnDoesFormContent = context.getResourceContent(
          "/versions/master/forms/GET/john-does-form.json");
      context.addFileToRemoteHeadRepo("/forms/john-does-form.json", johnDoesFormContent);
      final var mrSmithsFormContent = context.getResourceContent(
          "/versions/master/forms/GET/mr-smiths-form.json");
      context.addFileToRemoteHeadRepo("/forms/mr-smiths-form.json", mrSmithsFormContent);
      context.pullHeadRepo();
      context.addFileToRemoteHeadRepo("/forms/mr-smiths-form1.json", mrSmithsFormContent);

      // define expected john-does-form dates
      final var expectedJohnDoesFormDates = context.getHeadRepoDatesByPath(
          "forms/john-does-form.json");

      // perform query and expect only 2 of the processes that are pulled on head-branch repo
      mockMvc.perform(
          get("/versions/master/forms")
              .accept(MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$", hasSize(2)),
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

  @Nested
  @DisplayName("POST /versions/master/forms/{formName}")
  class MasterVersionFormsCreateFormControllerIT {

    @Test
    @DisplayName("should return 200 and create form if there's no such form")
    @SneakyThrows
    void createForm() {

      // define expected form content to create
      final var expectedFormContent = context.getResourceContent(
          "/versions/master/forms/{formName}/POST/valid-form-master.json");

      // perform query
      mockMvc.perform(
          post("/versions/master/forms/{formName}",
              "valid-form-master")
              .contentType(MediaType.APPLICATION_JSON)
              .content(expectedFormContent)
              .accept(MediaType.APPLICATION_JSON)
      ).andExpectAll(
          status().isCreated(),
          content().contentType(MediaType.APPLICATION_JSON),
          jsonPath("$.name", is("valid-form")),
          jsonPath("$.title", is("Valid form"))
      );

      // get created form
      var result = mockMvc.perform(
              get("/versions/master/forms/{formName}", "valid-form-master")
                  .accept(MediaType.APPLICATION_JSON)
          ).andExpectAll(
              status().isOk(),
              jsonPath("$.name", is("valid-form")),
              jsonPath("$.title", is("Valid form")))
          .andReturn()
          .getResponse();

      // assert that form dates are close to current date
      var form = JsonParser.parseString(result.getContentAsString()).getAsJsonObject();
      final var created = LocalDateTime.parse(form.get("created").getAsString(),
          JacksonConfig.DATE_TIME_FORMATTER).format(JacksonConfig.DATE_TIME_FORMATTER);
      final var updated = LocalDateTime.parse(form.get("modified").getAsString(),
          JacksonConfig.DATE_TIME_FORMATTER).format(JacksonConfig.DATE_TIME_FORMATTER);
      Assertions.assertThat(LocalDateTime.parse(created, JacksonConfig.DATE_TIME_FORMATTER))
          .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
      Assertions.assertThat(LocalDateTime.parse(updated, JacksonConfig.DATE_TIME_FORMATTER))
          .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
    }
  }
}
