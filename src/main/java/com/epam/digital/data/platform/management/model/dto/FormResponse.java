package com.epam.digital.data.platform.management.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FormResponse {
  private String name;
  private FormStatus status;
}
