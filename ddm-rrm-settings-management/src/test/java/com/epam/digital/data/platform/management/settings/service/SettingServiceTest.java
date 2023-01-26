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

package com.epam.digital.data.platform.management.settings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;

import com.epam.digital.data.platform.management.core.context.VersionContextComponentManager;
import com.epam.digital.data.platform.management.filemanagement.service.VersionedFileRepository;
import com.epam.digital.data.platform.management.settings.exception.SettingsParsingException;
import com.epam.digital.data.platform.management.settings.model.SettingsInfoDto;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class SettingServiceTest {

  private static final String VERSION_ID = "version";
  private static final String GLOBAL_VARS_PATH = "global-vars/camunda-global-system-vars.yml";
  private static final String SETTINGS_PATH = "settings/settings.yml";

  @Captor
  private ArgumentCaptor<String> captor;

  @Mock
  private VersionContextComponentManager versionContextComponentManager;
  @Mock
  private VersionedFileRepository repository;
  @InjectMocks
  private SettingServiceImpl settingServiceImpl;

  private static final String GLOBAL_SETTINGS_VALUE =
      "supportEmail: \"support@registry.gov.ua\"\n" +
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

  private static final String SETTINGS_EMPTY_CONTENT = "settings:\n" +
      "  general:\n" +
      "    titleFull: null\n" +
      "    title: null\n";
  private static final String GLOBAL_SETTINGS_EMPTY_VALUE = "themeFile: null\n" +
      "supportEmail: null\n";


  @BeforeEach
  @SneakyThrows
  void beforeEach() {
    Mockito.when(versionContextComponentManager.getComponent(VERSION_ID, VersionedFileRepository.class))
        .thenReturn(repository);
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
        .writeFile(eq("settings/settings.yml"), captor.capture());
    String settingsContent = captor.getValue();
    Mockito.verify(repository)
        .writeFile(eq("global-vars/camunda-global-system-vars.yml"), captor.capture());
    String globalVars = captor.getValue();
    Assertions.assertThat(settingsContent).isEqualTo(SETTINGS_EMPTY_CONTENT);
    Assertions.assertThat(globalVars).isEqualTo(GLOBAL_SETTINGS_EMPTY_VALUE);
    //check if there is no error, but not real value
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

  @Test
  @SneakyThrows
  void getSettingsInvalidContent() {
    Mockito.when(repository.readFile(GLOBAL_VARS_PATH))
        .thenReturn("Illegal settings");
    Mockito.when(repository.readFile(SETTINGS_PATH))
        .thenReturn("Illegal global vars");
    assertThatThrownBy(() -> settingServiceImpl.getSettings(VERSION_ID))
        .isInstanceOf(SettingsParsingException.class)
        .hasMessage("Could not process settings files");
  }
}