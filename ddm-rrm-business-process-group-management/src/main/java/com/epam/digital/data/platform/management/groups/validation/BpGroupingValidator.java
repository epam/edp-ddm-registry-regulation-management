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
package com.epam.digital.data.platform.management.groups.validation;

import com.epam.digital.data.platform.management.groups.exception.GroupDuplicateProcessDefinitionException;
import com.epam.digital.data.platform.management.groups.exception.GroupEmptyProcessDefinitionException;
import com.epam.digital.data.platform.management.groups.exception.GroupNameRegexException;
import com.epam.digital.data.platform.management.groups.exception.GroupNameRequiredException;
import com.epam.digital.data.platform.management.groups.exception.GroupNameUniqueException;
import com.epam.digital.data.platform.management.groups.exception.GroupsRequiredException;
import com.epam.digital.data.platform.management.groups.model.GroupDetails;
import com.epam.digital.data.platform.management.groups.model.GroupListDetails;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class BpGroupingValidator {

  private static final String GROUP_NAME_REGEXP = "^[А-ЩЬЮЯҐЄІЇа-щьюяґєії0-9 '`‘’()—–/:;№,.\\\\-]{3,512}$";

  public void validate(GroupListDetails groupListDetails) {
    final var groups = groupListDetails.getGroups();
    validGroups(groups);
    validProcessDefinitions(groupListDetails.getUngrouped());
    final var allGroupsProcessDefinitions = groups.stream()
        .map(GroupDetails::getProcessDefinitions)
        .filter(Objects::nonNull)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());

    if (!isUnique(Stream.concat(groupListDetails.getUngrouped().stream(),
        allGroupsProcessDefinitions.stream()).collect(Collectors.toList()))) {
      throw new GroupDuplicateProcessDefinitionException("Has found process definition duplicate");
    }
  }

  private static void validGroups(List<GroupDetails> groups) {
    if (groups == null) {
      throw new GroupsRequiredException("Groups are mandatory field");
    }
    var groupNames = groups.stream().map(GroupDetails::getName).collect(Collectors.toList());

    for (var group : groups) {
      if (group != null) {
        validaGroupName(group);
        validProcessDefinitions(group.getProcessDefinitions());
      }
    }
    if (!isUnique(groupNames)) {
      throw new GroupNameUniqueException("Groups name has to be unique");
    }
  }

  private static void validaGroupName(GroupDetails group) {
    final var name = group.getName();
    if (name == null) {
      throw new GroupNameRequiredException("Group name is mandatory");
    }

    if (!Pattern.matches(GROUP_NAME_REGEXP, name)) {
      throw new GroupNameRegexException("Name is not match with regex");
    }
  }

  private static void validProcessDefinitions(List<String> processDefinitions) {
    if (processDefinitions != null) {
      for (var processDefinition : processDefinitions) {
        if (processDefinition == null || processDefinition.isEmpty()) {
          throw new GroupEmptyProcessDefinitionException("Process definition cannot be empty");
        }
      }
    }
  }

  private static boolean isUnique(List<String> list) {
    var duplicates =
        list.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet()
            .stream()
            .filter(e -> e.getValue() > 1)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    return duplicates.isEmpty();
  }

}
