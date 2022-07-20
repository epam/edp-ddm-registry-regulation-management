package com.epam.digital.data.platform.poc.versioning.api.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RobotCommentRequestDto {
  private String robotId;
  private String robotRunId;
  private String comment;
  private String message;
  private String filePath;
}
