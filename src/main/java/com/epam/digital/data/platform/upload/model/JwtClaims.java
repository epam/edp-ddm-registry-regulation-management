/*
 * Copyright 2022 EPAM Systems.
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

package com.epam.digital.data.platform.upload.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JwtClaims {

  @JsonProperty("sub")
  protected String subject;
  private String edrpou;
  private String drfo;
  private String fullName;

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getEdrpou() {
    return edrpou;
  }

  public void setEdrpou(String edrpou) {
    this.edrpou = edrpou;
  }

  public String getDrfo() {
    return drfo;
  }

  public void setDrfo(String drfo) {
    this.drfo = drfo;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }
}
