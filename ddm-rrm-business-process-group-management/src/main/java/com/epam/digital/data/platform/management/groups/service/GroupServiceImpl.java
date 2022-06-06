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

import com.epam.digital.data.platform.management.core.context.VersionContextComponentManager;
import com.epam.digital.data.platform.management.core.service.CacheService;
import com.epam.digital.data.platform.management.filemanagement.service.VersionedFileRepository;
import com.epam.digital.data.platform.management.groups.exception.GroupsParseException;
import com.epam.digital.data.platform.management.groups.model.BusinessProcessDefinition;
import com.epam.digital.data.platform.management.groups.model.BusinessProcessGroupsResponse;
import com.epam.digital.data.platform.management.groups.model.GroupChangesDetails;
import com.epam.digital.data.platform.management.groups.model.GroupDetailsResponse;
import com.epam.digital.data.platform.management.groups.model.GroupListDetails;
import com.epam.digital.data.platform.management.service.BusinessProcessService;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GroupServiceImpl implements GroupService {

  private static final String GROUPS_PATH = "bp-grouping/bp-grouping.yml";
  private final VersionContextComponentManager versionContextComponentManager;
  private final BusinessProcessService businessProcessService;
  private final CacheService cacheService;

  @Override
  public BusinessProcessGroupsResponse getGroupsByVersion(String versionId) {
    final var processesByVersion = businessProcessService.getProcessesByVersion(
        versionId);
    final var processes = processesByVersion.stream()
        .map(process -> BusinessProcessDefinition.builder()
            .id(process.getName())
            .name(process.getTitle())
            .build())
        .collect(Collectors.toList());

    final var parseGroupFile = getGroupListDetails(versionId);
    var groups = new ArrayList<GroupDetailsResponse>();
    parseGroupFile.getGroups().forEach(group -> {
      var definitions = processBpDefinitions(group.getProcessDefinitions(), processes);
      groups.add(GroupDetailsResponse.builder()
          .name(group.getName())
          .processDefinitions(definitions)
          .build());
    });

    var ungrouped = processBpDefinitions(parseGroupFile.getUngrouped(), processes);
    processes.sort(Comparator.comparing(BusinessProcessDefinition::getName));
    ungrouped.addAll(processes);

    return BusinessProcessGroupsResponse.builder().groups(groups).ungrouped(ungrouped).build();
  }

  @Override
  public void save(String versionId, GroupListDetails groupDetails) {
    var mapper =
        new YAMLMapper(new YAMLFactory())
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .configure(YAMLGenerator.Feature.SPLIT_LINES, false);
    log.debug("YAMLMapper was initialized. Trying to get repo");
    var repo = versionContextComponentManager.getComponent(versionId,
        VersionedFileRepository.class);
    log.debug("Finished getting repo for {} version", versionId);

    try {
      log.debug("Writing settings to file");
      repo.writeFile(GROUPS_PATH, mapper.writeValueAsString(groupDetails));
      log.debug("Finished writing settings");
    } catch (JsonProcessingException exception) {
      throw new GroupsParseException("Could not process bp-grouping file", exception);
    }
  }

  @Override
  public GroupChangesDetails getChangesByVersion(String versionId) {
    var repo = versionContextComponentManager.getComponent(versionId,
        VersionedFileRepository.class);
    List<String> conflicts = cacheService.getConflictsCache(versionId);

    return repo.getFileList(GROUPS_PATH).stream()
        .filter(grouping -> "bp-grouping".equals(grouping.getName()))
        .map(grouping -> GroupChangesDetails.builder()
            .name(grouping.getName() + ".yml")
            .status(grouping.getStatus())
            .conflicted(conflicts.contains(grouping.getPath()))
            .build())
        .findFirst()
        .orElse(null);
  }

  @Override
  public void deleteProcessDefinition(String processDefinitionId, String versionCandidateId) {
    final var groupsByVersion = getGroupListDetails(versionCandidateId);
    final var groups = groupsByVersion.getGroups();
    AtomicBoolean deleted = new AtomicBoolean(false);
    if (groups != null) {
      groupsByVersion.getGroups().stream()
          .filter(group -> group.getProcessDefinitions() != null)
          .forEach(group -> {
            if (group.getProcessDefinitions().remove(processDefinitionId)) {
              deleted.set(true);
            }
          });
    }
    if (groupsByVersion.getUngrouped() != null && groupsByVersion.getUngrouped()
        .remove(processDefinitionId)) {
      deleted.set(true);
    }
    if (deleted.get()) {
      save(versionCandidateId, groupsByVersion);
    }
  }

  @Override
  public void rollbackBusinessProcessGroups(String versionId) {
    var repo = versionContextComponentManager.getComponent(versionId,
        VersionedFileRepository.class);
    repo.rollbackFile(GROUPS_PATH);
  }

  private List<BusinessProcessDefinition> processBpDefinitions(List<String> definitionsFromFile,
      List<BusinessProcessDefinition> processes) {
    var definitions = new ArrayList<BusinessProcessDefinition>();
    definitionsFromFile.forEach(def -> {
      final var businessProcessDefinition = processes.stream()
          .filter(pr -> def.equals(pr.getId())).findFirst().orElse(null);
      if (businessProcessDefinition != null) {
        definitions.add(businessProcessDefinition);
        processes.remove(businessProcessDefinition);
      }
    });
    return definitions;
  }

  private GroupListDetails getGroupListDetails(String versionId) {
    log.debug("Trying to get repo");
    var repo = versionContextComponentManager.getComponent(versionId,
        VersionedFileRepository.class);
    log.debug("Finished getting repo for {} version", versionId);
    var groupFileContent = repo.readFile(GROUPS_PATH);
    log.debug("Completed business process groups reading");
    return getParseGroupFile(groupFileContent);
  }

  private GroupListDetails getParseGroupFile(String groupFileContent) {
    var mapper = new ObjectMapper(new YAMLFactory())
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(JsonGenerator.Feature.IGNORE_UNKNOWN);
    log.debug("ObjectMapper was initialized");
    try {
      log.debug("Parsing business process groups file");
      return mapper.readValue(groupFileContent, GroupListDetails.class);
    } catch (JsonProcessingException exception) {
      throw new GroupsParseException("Could not process bp-grouping file", exception);
    }
  }
}
