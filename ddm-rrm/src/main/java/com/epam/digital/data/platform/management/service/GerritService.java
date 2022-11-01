/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.digital.data.platform.management.service;

import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.epam.digital.data.platform.management.model.dto.CreateVersionRequest;
import com.epam.digital.data.platform.management.model.dto.RobotCommentRequestDto;
import com.epam.digital.data.platform.management.model.dto.VoteRequestDto;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.FileInfo;
import com.google.gerrit.extensions.restapi.RestApiException;

import java.util.List;
import java.util.Map;
import org.springframework.lang.Nullable;

public interface GerritService {
  List<ChangeInfo> getMRList() throws RestApiException;
  @Nullable
  ChangeInfo getLastMergedMR() throws RestApiException;
  List<String> getClosedMrIds() throws RestApiException;
  ChangeInfo getMRByNumber(String number) throws RestApiException;
  ChangeInfoDto getChangeInfo(String changeId) throws RestApiException;
  Map<String, FileInfo> getListOfChangesInMR(String changeId);
  String getFileContent(String changeId, String filename) throws RestApiException;
  void submitChanges(String changeId);
  void deleteChanges(String changeId) throws RestApiException;
  String createChanges(CreateVersionRequest subject) throws RestApiException;
  Boolean review(String changeId);
  Boolean vote(VoteRequestDto voteRequestDto, String changeId) throws RestApiException;
  void declineChange(String changeId);
  void rebase(String changeId) throws RestApiException;
  void robotComment(RobotCommentRequestDto requestDto, String changeId) throws RestApiException;

  String getTopic(String changeId) throws RestApiException;

  void setTopic(String text, String changeId) throws RestApiException;
}
