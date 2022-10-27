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
package data.model.snapshot.model.converter;

import data.model.snapshot.model.DdmRolePermissionOperation;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class OperationConverter implements AttributeConverter<DdmRolePermissionOperation, String> {

  @Override
  public String convertToDatabaseColumn(DdmRolePermissionOperation attribute) {
    return attribute.getCode();
  }

  @Override
  public DdmRolePermissionOperation convertToEntityAttribute(String dbData) {
    return DdmRolePermissionOperation.of(dbData);
  }
}
