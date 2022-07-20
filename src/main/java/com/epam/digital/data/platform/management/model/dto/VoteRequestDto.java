package com.epam.digital.data.platform.poc.versioning.api.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VoteRequestDto {
  private String label;
  private Short value;
}
