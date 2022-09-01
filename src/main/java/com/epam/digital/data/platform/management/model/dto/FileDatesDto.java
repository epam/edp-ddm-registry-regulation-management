package com.epam.digital.data.platform.management.model.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class FileDatesDto {
  LocalDateTime create;
  LocalDateTime update;
}
