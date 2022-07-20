package com.epam.digital.data.platform.poc.versioning.api.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FormsResponseDto {
  private String name;
  private FormStatus status;
}
