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
package com.epam.digital.data.platform.management.gerritintegration.service;

import com.epam.digital.data.platform.management.gerritintegration.exception.GerritChangeNotFoundException;
import com.epam.digital.data.platform.management.gerritintegration.exception.GerritCommunicationException;
import com.epam.digital.data.platform.management.gerritintegration.exception.GerritConflictException;
import com.epam.digital.data.platform.management.gerritintegration.model.ChangeInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.model.ChangeInfoShortDto;
import com.epam.digital.data.platform.management.gerritintegration.model.CreateChangeInputDto;
import com.epam.digital.data.platform.management.gerritintegration.model.FileInfoDto;
import com.epam.digital.data.platform.management.gerritintegration.model.RobotCommentInputDto;

import java.util.List;
import java.util.Map;
import org.springframework.lang.Nullable;

/**
 * Provides methods for working with gerrit.
 */
public interface GerritService {

  /**
   * Get list of changes
   *
   * @return {@link List} of {@link ChangeInfoDto}
   *
   * @throws GerritChangeNotFoundException in case when gerrit returns 404 http status
   * @throws GerritCommunicationException  in case of rest http errors or gerrit issues
   */
  List<ChangeInfoShortDto> getMRList();

  /**
   * Returns last merged change
   *
   * @return {@link ChangeInfoDto} change info
   *
   * @throws GerritChangeNotFoundException in case when gerrit returns 404 http status
   * @throws GerritCommunicationException  in case of rest http errors or gerrit issues
   */
  @Nullable
  ChangeInfoDto getLastMergedMR();

  /**
   * Returns ids of submitted changes
   *
   * @return {@link List} of {@link String}
   *
   * @throws GerritChangeNotFoundException in case when gerrit returns 404 http status
   * @throws GerritCommunicationException  in case of rest http errors or gerrit issues
   */
  List<String> getClosedMrIds();

  /**
   * Get gerrit change information by number
   *
   * @param number unique identifier of change
   * @return {@link ChangeInfoDto} change info
   */
  ChangeInfoDto getMRByNumber(String number);

  /**
   * Get gerrit change information by changeId
   *
   * @param changeId unique identifier of change
   * @return {@link ChangeInfoDto} change info
   *
   * @throws GerritChangeNotFoundException in case when gerrit returns 404 http status
   * @throws GerritCommunicationException  in case of rest http errors or gerrit issues
   */
  ChangeInfoDto getChangeInfo(String changeId);

  /**
   * Get information about files in gerrit change
   *
   * @param changeId unique identifier of change
   * @return {@link Map} of {@link String} file name and file info
   *
   * @throws GerritChangeNotFoundException in case when gerrit returns 404 http status
   * @throws GerritCommunicationException  in case of rest http errors or gerrit issues
   */
  Map<String, FileInfoDto> getListOfChangesInMR(String changeId);

  /**
   * Get content of file
   *
   * @param changeId unique identifier of change
   * @param filename name of file
   * @return {@link String} file content
   *
   * @throws GerritChangeNotFoundException in case when gerrit returns 404 http status
   * @throws GerritCommunicationException  in case of rest http errors or gerrit issues
   */
  String getFileContent(String changeId, String filename);

  /**
   * Submit gerrit change by chnageId
   *
   * @param changeId unique identifier of change
   * @throws GerritChangeNotFoundException in case when gerrit returns 404 http status
   * @throws GerritCommunicationException  in case of rest http errors or gerrit issues
   * @throws GerritConflictException       in case when gerrit returns 409 http status
   */
  void submitChanges(String changeId);

  /**
   * Delete change from gerrit
   *
   * @param changeId unique identifier of change
   * @throws GerritChangeNotFoundException in case when gerrit returns 404 http status
   * @throws GerritCommunicationException  in case of rest http errors or gerrit issues
   */
  void deleteChanges(String changeId);

  /**
   * Create change by date from payload
   *
   * @param dto payload with change information
   * @return {@link String} number of new gerrit change
   *
   * @throws GerritChangeNotFoundException in case when gerrit returns 404 http status
   * @throws GerritCommunicationException  in case of rest http errors or gerrit issues
   */
  String createChanges(CreateChangeInputDto dto);

  /**
   * Makes gerrit change reviewed
   *
   * @param changeId unique identifier of change
   * @return {@link Boolean} is it possible to submit the change
   *
   * @throws GerritChangeNotFoundException in case when gerrit returns 404 http status
   * @throws GerritCommunicationException  in case of rest http errors or gerrit issues
   */
  Boolean review(String changeId);

  /**
   * Decline change by changeId
   *
   * @param changeId unique identifier of change
   * @throws GerritChangeNotFoundException in case when gerrit returns 404 http status
   * @throws GerritCommunicationException  in case of rest http errors or gerrit issues
   */
  void declineChange(String changeId);

  /**
   * Do rebase on gerrit change
   *
   * @param changeId unique identifier of change
   * @throws GerritCommunicationException in case of rest http errors or gerrit issues
   */
  void rebase(String changeId);

  /**
   * Create robot comment on the change with changeId
   *
   * @param requestDto payload to create robot comment
   * @param changeId   unique identifier of change
   * @throws GerritChangeNotFoundException in case when gerrit returns 404 http status
   * @throws GerritCommunicationException  in case of rest http errors or gerrit issues
   */
  void robotComment(RobotCommentInputDto requestDto, String changeId);

  /**
   * Returns topic from gerrit change
   *
   * @param changeId unique identifier of change
   * @return {@link String} topic value
   *
   * @throws GerritChangeNotFoundException in case when gerrit returns 404 http status
   * @throws GerritCommunicationException  in case of rest http errors or gerrit issues
   */
  String getTopic(String changeId);

  /**
   * Set topic to gerrit change
   *
   * @param text     topic content
   * @param changeId unique identifier of change
   * @throws GerritChangeNotFoundException in case when gerrit returns 404 http status
   * @throws GerritCommunicationException  in case of rest http errors or gerrit issues
   */
  void setTopic(String text, String changeId);
}
