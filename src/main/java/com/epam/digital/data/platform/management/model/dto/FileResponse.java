package com.epam.digital.data.platform.management.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@Builder
public class FileResponse {
  private String name;
  private String path;
  private FileStatus status;
  private Timestamp created;
  private Timestamp updated;
}
