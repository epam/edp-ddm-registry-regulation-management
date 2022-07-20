package com.epam.digital.data.platform.management.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VoteRequestDto {
  private String label;
  private Short value;
}
