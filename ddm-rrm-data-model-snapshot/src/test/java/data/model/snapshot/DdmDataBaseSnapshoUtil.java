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
package data.model.snapshot;

import data.model.snapshot.model.DdmColumn;
import data.model.snapshot.model.DdmDataBaseSnapshot;
import data.model.snapshot.model.DdmIndex.Column;
import data.model.snapshot.model.DdmIndex.Column.Sorting;
import data.model.snapshot.model.DdmPrimaryKeyConstraint;
import data.model.snapshot.model.DdmRolePermission;
import data.model.snapshot.model.DdmRolePermissionOperation;
import data.model.snapshot.model.DdmTable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DdmDataBaseSnapshoUtil {

  public static DdmDataBaseSnapshot getSnapshot() {
    DdmDataBaseSnapshot snapshot = new DdmDataBaseSnapshot();
    snapshot.setDdmRolePermissions(getDdmRolePermissions());
    snapshot.setDdmTables(getDdmTables());
    return snapshot;
  }

  public static Map<String, DdmTable> getDdmTables() {
    Map<String, DdmTable> map = new HashMap<>();
    DdmTable table = new DdmTable("table");
    table.setDescription("Table description");
    table.setHistoryFlag(false);
    table.setObjectReference(false);
    table.setPrimaryKey(getPrimaryKey());
    table.setColumns(getTableColumns());
    map.put(table.getName(), table);
    return map;
  }

  public static Map<String, DdmColumn> getTableColumns() {
    Map<String, DdmColumn> columns = new HashMap<>();
    DdmColumn column = new DdmColumn();
    column.setTableName("table");
    column.setName("column");
    column.setDescription("Column description");
    column.setType("text");
    column.setDefaultValue("default");
    columns.put(column.getName(), column);
    return columns;
  }

  private static DdmPrimaryKeyConstraint getPrimaryKey() {
    DdmPrimaryKeyConstraint pk = new DdmPrimaryKeyConstraint();
    Column column = new Column();
    column.setName("id");
    column.setSorting(Sorting.ASC);
    pk.setColumns(List.of(column));
    return pk;
  }

  private static Map<Integer, DdmRolePermission> getDdmRolePermissions() {
    DdmRolePermission permission = new DdmRolePermission();
    permission.setPermissionId(1);
    permission.setColumnName("column_name");
    permission.setRoleName("role_name");
    permission.setOperation(DdmRolePermissionOperation.SELECT);
    permission.setObjectName("object_name");
    Map<Integer, DdmRolePermission> permissionMap = new HashMap<>();
    permissionMap.put(permission.getPermissionId(), permission);
    return permissionMap;
  }

}
