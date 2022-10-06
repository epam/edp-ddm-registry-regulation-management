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

package com.epam.digital.data.platform.management.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode
public class SettingDto {

  private Settings settings = new Settings();

  public SettingDto() {
  }

  public SettingDto(String titleFull, String title, List<String> domains) {
    settings.getGeneral().getValidation().getEmail().getBlacklist().setDomains(domains);
    settings.getGeneral().setTitleFull(titleFull);
    settings.getGeneral().setTitle(title);
  }

  @JsonIgnore
  public List<String> getBlacklistedDomains() {
    return settings.general.validation.email.blacklist.domains;
  }

  @JsonIgnore
  public String getTitleFull() {
    return settings.getGeneral().titleFull;
  }

  @JsonIgnore
  public String getTitle() {
    return settings.getGeneral().title;
  }

  @Getter
  @Setter
  private static class Settings {
    private General general = new General();
  }

  @Getter
  @Setter
  private static class General {
    private Validation validation = new Validation();
    private String titleFull;
    private String title;
  }

  @Getter
  @Setter
  private static class Validation {
    private Email email = new Email();
  }

  @Getter
  @Setter
  private static class Blacklist {
    private List<String> domains;
  }

  @Getter
  @Setter
  private static class Email {
    private Blacklist blacklist = new Blacklist();
  }
}
