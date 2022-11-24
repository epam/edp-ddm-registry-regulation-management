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

package com.epam.digital.data.platform.management.validation.businessProcess;

import java.io.IOException;
import java.io.StringReader;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.SAXException;

public class BusinessProcessValidator implements ConstraintValidator<BusinessProcess, String> {

  public static final String SCHEMA_PATH = "/org/camunda/bpm/model/bpmn/schema/";
  public static final String DDM_BP_SCHEMA = SCHEMA_PATH + "bp-schema.xsd";

  @Override
  public boolean isValid(String bpContent, ConstraintValidatorContext constraintValidatorContext) {
    try (var businessProcessReader = new StringReader(bpContent)) {
      var validator = initValidator();
      validator.validate(new StreamSource(businessProcessReader));
    } catch (SAXException | IOException e) {
      constraintValidatorContext.disableDefaultConstraintViolation();
      constraintValidatorContext.buildConstraintViolationWithTemplate(e.getMessage())
          .addConstraintViolation();
      return false;
    }
    return true;
  }

  private Validator initValidator() throws SAXException, IOException {
    var resourceDdm = getClass().getResource(DDM_BP_SCHEMA);
    var factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    factory.setResourceResolver(new SchemaResolver(SCHEMA_PATH));
    var schema = factory.newSchema(new StreamSource(resourceDdm.toExternalForm()));
    return schema.newValidator();
  }
}
