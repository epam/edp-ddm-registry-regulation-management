package com.epam.digital.data.platform.management.gerritintegration.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileInfoDto {
  private String status;
  private Integer linesInserted;
  private Integer linesDeleted;
  private long size;
  private long sizeDelta;
}
