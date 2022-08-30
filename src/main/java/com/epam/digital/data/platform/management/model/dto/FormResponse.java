package com.epam.digital.data.platform.management.model.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
public class FormResponse {
  private String name;
  private String title;
  private String path;
  private FileStatus status;
  private LocalDateTime created;
  private LocalDateTime updated;
}
