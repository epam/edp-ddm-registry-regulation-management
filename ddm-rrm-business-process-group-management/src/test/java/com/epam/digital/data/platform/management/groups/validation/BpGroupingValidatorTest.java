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
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class BpGroupingValidatorTest {


  @InjectMocks
  private BpGroupingValidator validator;

  @Test
  void validateSuccessTest() {
    Assertions.assertThatCode(() -> validator.validate(getGroupListDetails()))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldThrowGroupsRequiredException() {
    Assertions.assertThatCode(() -> validator.validate(nullGroups()))
        .isInstanceOf(GroupsRequiredException.class)
        .hasMessage("Groups are mandatory field");
  }

  @Test
  void shouldTrowGroupEmptyProcessDefinitionException() {
    Assertions.assertThatCode(() -> validator.validate(processDefinitionEmpty()))
        .isInstanceOf(GroupEmptyProcessDefinitionException.class)
        .hasMessage("Process definition cannot be empty");
  }

  @Test
  void shouldTrowGroupDuplicateProcessDefinitionException() {
    Assertions.assertThatCode(() -> validator.validate(duplicateProcessDefinition()))
        .isInstanceOf(GroupDuplicateProcessDefinitionException.class)
        .hasMessage("Has found process definition duplicate");
  }

  @Test
  void shouldTrowGroupNameRegexException() {
    Assertions.assertThatCode(() -> validator.validate(nameContainsForbiddenCharacters()))
        .isInstanceOf(GroupNameRegexException.class)
        .hasMessage("Name is not match with regex");
  }

  @Test
  void shouldTrowGroupNameRequiredException() {
    Assertions.assertThatCode(() -> validator.validate(nullGroupName()))
        .isInstanceOf(GroupNameRequiredException.class)
        .hasMessage("Group name is mandatory");
  }

  @Test
  void shouldTrowGroupNameUniqueException() {
    Assertions.assertThatCode(() -> validator.validate(notUniqueGroupName()))
        .isInstanceOf(GroupNameUniqueException.class)
        .hasMessage("Groups name has to be unique");
  }

  private static GroupListDetails nullGroups() {
    return GroupListDetails.builder()
        .ungrouped(List.of("bp-4-process_definition_id", "bp-5-process_definition_id")).build();
  }

  private static GroupListDetails nameContainsForbiddenCharacters() {
    return GroupListDetails.builder()
        .groups(List.of(GroupDetails.builder().name("First group")
                .processDefinitions(List.of("bp-1-process_definition_id", "bp-2-process_definition_id"))
                .build(),
            GroupDetails.builder().name("Перша група")
                .processDefinitions(List.of("bp-3-process_definition_id")).build(),
            GroupDetails.builder().name("Третя група").build()))
        .ungrouped(List.of("bp-4-process_definition_id", "bp-5-process_definition_id")).build();
  }

  private static GroupListDetails notUniqueGroupName() {
    return GroupListDetails.builder()
        .groups(List.of(GroupDetails.builder().name("Перша група")
                .processDefinitions(List.of("bp-1-process_definition_id", "bp-2-process_definition_id"))
                .build(),
            GroupDetails.builder().name("Перша група")
                .processDefinitions(List.of("bp-3-process_definition_id")).build(),
            GroupDetails.builder().name("Третя група").build()))
        .ungrouped(List.of("bp-4-process_definition_id", "bp-5-process_definition_id")).build();
  }

  private static GroupListDetails nullGroupName() {
    return GroupListDetails.builder()
        .groups(List.of(GroupDetails.builder()
                .processDefinitions(List.of("bp-1-process_definition_id", "bp-2-process_definition_id"))
                .build(),
            GroupDetails.builder().name("Друга група")
                .processDefinitions(List.of("bp-3-process_definition_id")).build(),
            GroupDetails.builder().name("Третя група").build()))
        .ungrouped(List.of("bp-4-process_definition_id", "bp-5-process_definition_id")).build();
  }

  private static GroupListDetails duplicateProcessDefinition() {
    return GroupListDetails.builder()
        .groups(List.of(GroupDetails.builder().name("Перша група")
                .processDefinitions(List.of("bp-1-process_definition_id", "bp-2-process_definition_id"))
                .build(),
            GroupDetails.builder().name("Друга група")
                .processDefinitions(List.of("bp-1-process_definition_id")).build(),
            GroupDetails.builder().name("Третя група").build()))
        .ungrouped(List.of("bp-4-process_definition_id", "bp-5-process_definition_id")).build();
  }

  private static GroupListDetails processDefinitionEmpty() {
    return GroupListDetails.builder()
        .groups(List.of(GroupDetails.builder().name("Перша група")
                .processDefinitions(List.of("bp-1-process_definition_id", "bp-2-process_definition_id"))
                .build(),
            GroupDetails.builder().name("Друга група")
                .processDefinitions(List.of("")).build(),
            GroupDetails.builder().name("Третя група").build()))
        .ungrouped(List.of("bp-4-process_definition_id", "bp-5-process_definition_id")).build();
  }

  private static GroupListDetails getGroupListDetails() {
    return GroupListDetails.builder()
        .groups(List.of(GroupDetails.builder().name("Перша група")
                .processDefinitions(List.of("bp-1-process_definition_id", "bp-2-process_definition_id"))
                .build(),
            GroupDetails.builder().name("Друга група")
                .processDefinitions(List.of("bp-3-process_definition_id")).build(),
            GroupDetails.builder().name("Третя група").build()))
        .ungrouped(List.of("bp-4-process_definition_id", "bp-5-process_definition_id")).build();
  }
}
