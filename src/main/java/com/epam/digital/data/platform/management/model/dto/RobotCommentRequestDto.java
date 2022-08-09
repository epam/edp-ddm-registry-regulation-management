package com.epam.digital.data.platform.management.model.dto;

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
