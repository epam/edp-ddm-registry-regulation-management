/*
 * Copyright 2023 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.digital.data.platform.management.groups.service;

import com.epam.digital.data.platform.management.groups.exception.GroupsParseException;
import com.epam.digital.data.platform.management.groups.model.BusinessProcessGroupsResponse;
import com.epam.digital.data.platform.management.groups.model.GroupChangesDetails;
import com.epam.digital.data.platform.management.groups.model.GroupListDetails;

/**
 * Provides methods to access to bp-grouping file
 */
public interface GroupService {

  /**
   * Return bp-grouping for certain version
   *
   * @param versionId version identifier
   * @return {@link BusinessProcessGroupsResponse} representation of bp-grouping file
   *
   * @throws GroupsParseException when grouping file has invalid structure
   */
  BusinessProcessGroupsResponse getGroupsByVersion(String versionId);

  /**
   * Save {@link GroupListDetails} to bp-grouping file
   *
   * @param versionId    version candidate identifier
   * @param groupDetails business process groups details
   *
   * @throws GroupsParseException when grouping file has invalid structure
   */
  void save(String versionId, GroupListDetails groupDetails);

  /**
   * Returns changes between version candidate and master version
   * @param versionId version candidate identifier
   * @return {@link GroupChangesDetails}
   */
  GroupChangesDetails getChangesByVersion(String versionId);

  /**
   * Deletes process definitions from group
   * @param processDefinitionId process definition identifier
   * @param versionCandidateId version candidate identifier
   */
  void deleteProcessDefinition(String processDefinitionId, String versionCandidateId);

  /**
   * Rolls back bp-grouping file to a specific version.
   *
   * @param versionId version candidate identifier
   */
  void rollbackBusinessProcessGroups(String versionId);
}
