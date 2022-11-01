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
package com.epam.digital.data.platform.management.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.management.exception.ApplicationExceptionHandler;
import com.epam.digital.data.platform.management.exception.TableNotFoundException;
import com.epam.digital.data.platform.management.exception.TableParseException;
import com.epam.digital.data.platform.management.model.dto.TableDetailsShort;
import com.epam.digital.data.platform.management.service.TableService;
import com.epam.digital.data.platform.starter.localization.MessageResolver;
import data.model.snapshot.model.DdmTable;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ControllerTest({MasterVersionTableController.class, ApplicationExceptionHandler.class})
public class MasterVersionTableControllerTest {

  static final String BASE_URL = "/versions/master/tables";

  @MockBean
  TableService tableService;

  @MockBean
  private MessageResolver messageResolver;

  MockMvc mockMvc;

  @RegisterExtension
  final RestDocumentationExtension restDocumentation = new RestDocumentationExtension();

  @BeforeEach
  public void setUp(WebApplicationContext webApplicationContext,
      RestDocumentationContextProvider restDocumentation) {

    this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
        .apply(documentationConfiguration(restDocumentation)).build();
  }


  @Test
  @SneakyThrows
  void getTablesTest() {
    var expectedTablesResponse = TableDetailsShort.builder()
        .name("John Doe's table")
        .description("John Doe get table")
        .objectReference(true)
        .historyFlag(false)
        .build();
    Mockito.when(tableService.list())
        .thenReturn(List.of(expectedTablesResponse));

    mockMvc.perform(get(BASE_URL))
        .andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.[0].name", is("John Doe's table")),
            jsonPath("$.[0].description", is("John Doe get table")),
            jsonPath("$.[0].objectReference", is(true)),
            jsonPath("$.[0].historyFlag", is(false)))
        .andDo(document("versions/master/tables/GET"));
    Mockito.verify(tableService).list();
  }

  @Test
  @SneakyThrows
  void getTableTest() {
    final String name = "John_Does_table";
    var expectedTablesResponse = new DdmTable();
    expectedTablesResponse.setName(name);
    expectedTablesResponse.setDescription("John Doe get table");
    expectedTablesResponse.setObjectReference(true);
    expectedTablesResponse.setHistoryFlag(false);
    Mockito.when(tableService.get(name))
        .thenReturn(expectedTablesResponse);

    mockMvc.perform(get(String.format("%s/%s", BASE_URL, name)))
        .andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.name", is(name)),
            jsonPath("$.description", is("John Doe get table")),
            jsonPath("$.objectReference", is(true)),
            jsonPath("$.historyFlag", is(false)))
        .andDo(document("versions/master/tables/{tableName}/GET"));
    Mockito.verify(tableService).get(name);
  }

  @Test
  @SneakyThrows
  void getTableNotFoundException() {
    final String name = "name";
    Mockito.when(tableService.get(name))
        .thenThrow(new TableNotFoundException(name));
    mockMvc.perform(get(String.format("%s/%s", BASE_URL, name)))
        .andExpectAll(
            status().isNotFound(),
            jsonPath("$.code").value(is("TABLE_NOT_FOUND_EXCEPTION")),
            content().contentType(MediaType.APPLICATION_JSON)
        );
    Mockito.verify(tableService).get(name);
  }

  @Test
  @SneakyThrows
  void getTableParseException() {
    final String name = "name";
    Mockito.when(tableService.get(name))
        .thenThrow(new TableParseException(name));
    mockMvc.perform(get(String.format("%s/%s", BASE_URL, name)))
        .andExpectAll(
            status().isInternalServerError(),
            jsonPath("$.code").value(is("TABLE_PARSE_EXCEPTION")),
            content().contentType(MediaType.APPLICATION_JSON)
        );
    Mockito.verify(tableService).get(name);
  }

}
