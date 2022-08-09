package com.epam.digital.data.platform.management.model.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FileResponse {
  private String name;
  private String path;
  private FileStatus status;
  private LocalDateTime created;
  private LocalDateTime updated;
}
