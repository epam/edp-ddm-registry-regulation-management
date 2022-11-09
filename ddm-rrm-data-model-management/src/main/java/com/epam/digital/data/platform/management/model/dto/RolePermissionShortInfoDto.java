/*
 * Copyright 2022 EPAM Systems.
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
package com.epam.digital.data.platform.management.model.dto;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ddm_role_permission", schema = "public")
@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode(of = "permissionId")
public class RolePermissionShortInfoDto {

  @Id
  @Column(name = "permission_id")
  private int permissionId;
  @Column(name = "role_name")
  private String roleName;
  @Column(name = "object_name")
  private String objectName;
  @Column(name = "column_name")
  private String columnName;
  @Column(name = "operation")
  private DdmRolePermissionOperation operation;

}
