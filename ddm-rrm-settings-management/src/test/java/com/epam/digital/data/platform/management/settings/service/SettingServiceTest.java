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

package com.epam.digital.data.platform.management.settings.service;

import com.epam.digital.data.platform.management.filemanagement.service.VersionedFileRepository;
import com.epam.digital.data.platform.management.filemanagement.service.VersionedFileRepositoryFactory;
import com.epam.digital.data.platform.management.settings.model.SettingsInfoDto;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.assertj.core.api.Assertions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(SpringExtension.class)
class SettingServiceTest {

  private static final String VERSION_ID = "version";
  private static final String GLOBAL_VARS_PATH = "global-vars/camunda-global-system-vars.yml";
  private static final String SETTINGS_PATH = "settings/settings.yml";

  @Mock
  private VersionedFileRepositoryFactory repositoryFactory;
  @Mock
  private VersionedFileRepository repository;
  @InjectMocks
  private SettingServiceImpl settingServiceImpl;

  private static final String GLOBAL_SETTINGS_VALUE = "supportEmail: \"support@registry.gov.ua\"\n" +
      "themeFile: \"white-theme.js\"\n";
  private static final String SETTINGS_VALUE = "settings:\n" +
      "  general:\n" +
      "    validation:\n" +
      "      email:\n" +
      "        blacklist:\n" +
      "          domains:\n" +
      "          - \"ya.ua\"\n" +
      "          - \"ya.ru\"\n" +
      "    titleFull: \"<Назва реєстру>\"\n" +
      "    title: \"mdtuddm\"\n";

  @BeforeEach
  @SneakyThrows
  void beforeEach() {
    Mockito.when(repositoryFactory.getRepoByVersion(VERSION_ID)).thenReturn(repository);
  }

  @Test
  @SneakyThrows
  void setSettingsTest() {
    Mockito.when(repository.readFile(GLOBAL_VARS_PATH))
        .thenReturn(GLOBAL_SETTINGS_VALUE);
    Mockito.when(repository.readFile(SETTINGS_PATH)).thenReturn(SETTINGS_VALUE);
    SettingsInfoDto expected = SettingsInfoDto.builder()
        .supportEmail("support@registry.gov.ua")
        .title("mdtuddm")
        .titleFull("<Назва реєстру>")
//        .blacklistedDomains(List.of("ya.ua", "ya.ru")) TODO uncomment after validator-cli update
        .themeFile("white-theme.js")
        .build();
    settingServiceImpl.updateSettings(VERSION_ID, expected);
    SettingsInfoDto actual = settingServiceImpl.getSettings(VERSION_ID);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @SneakyThrows
  void setSettingsNoErrorTest() {
    var settings = SettingsInfoDto.builder().build();
    Assertions.assertThatCode(() -> settingServiceImpl.updateSettings(VERSION_ID, settings))
        .doesNotThrowAnyException();
    Mockito.verify(repository)
        .writeFile(eq("settings/settings.yml"), anyString());
    Mockito.verify(repository)
        .writeFile(eq("global-vars/camunda-global-system-vars.yml"), anyString());
  }

  @Test
  @SneakyThrows
  void getSettings() {
    Mockito.when(repository.readFile(GLOBAL_VARS_PATH))
        .thenReturn(GLOBAL_SETTINGS_VALUE);
    Mockito.when(repository.readFile(SETTINGS_PATH)).thenReturn(SETTINGS_VALUE);
    SettingsInfoDto expected = SettingsInfoDto.builder()
        .supportEmail("support@registry.gov.ua")
        .title("mdtuddm")
        .titleFull("<Назва реєстру>")
//        .blacklistedDomains(List.of("ya.ua", "ya.ru")) TODO uncomment after validator-cli update
        .themeFile("white-theme.js")
        .build();
    SettingsInfoDto actual = settingServiceImpl.getSettings(VERSION_ID);
    assertThat(actual).isEqualTo(expected);
  }
}