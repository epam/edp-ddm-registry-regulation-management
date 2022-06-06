
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

package com.epam.digital.data.platform.management.validation;

import java.io.IOException;
import java.io.StringReader;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.xml.sax.SAXException;

/**
 * Validates change log file against liquibase schemas and liquibase extension schemas
 *
 * @see DDMExtensionChangelogFile
 */
@RequiredArgsConstructor
@Slf4j
public class DDMExtensionChangelogFileValidator implements
    ConstraintValidator<DDMExtensionChangelogFile, String> {

  private static final String SCHEMA_PATH = "/liquibase-schema";
  private static final String DB_CHANGELOG_SCHEMA = SCHEMA_PATH + "/dbchangelog.xsd";
  private static final String LIQUIBASE_EXT_SCHEMA = SCHEMA_PATH + "/liquibase-ext-schema.xsd";

  @Override
  public boolean isValid(String changeLogContent, ConstraintValidatorContext context) {
    try (var businessProcessReader = new StringReader(changeLogContent)) {
      var dbChangelog = initValidator(DB_CHANGELOG_SCHEMA);
      dbChangelog.validate(new StreamSource(businessProcessReader));
      // TODO uncomment when fix "Error for type 'whereType'. Multiple elements with name
      //  'condition', with different types, appear in the model group."
      //  var liquibaseExtValidator = initValidator(LIQUIBASE_EXT_SCHEMA);
      //  liquibaseExtValidator.validate(new StreamSource(businessProcessReader));
    } catch (SAXException | IOException e) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate(e.getMessage()).addConstraintViolation();
      return false;
    }
    return true;
  }

  private Validator initValidator(String dbChangeLogSchema) throws SAXException {
    var resourceDdm = getClass().getResource(dbChangeLogSchema);
    var factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    var schema = factory.newSchema(new StreamSource(resourceDdm.toExternalForm()));
    return schema.newValidator();
  }
}
