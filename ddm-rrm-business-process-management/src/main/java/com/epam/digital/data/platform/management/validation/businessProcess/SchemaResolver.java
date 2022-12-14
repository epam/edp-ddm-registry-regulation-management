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

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.xerces.dom.DOMInputImpl;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

@RequiredArgsConstructor
public class SchemaResolver implements LSResourceResolver {

  private final String baseSchemaPath;

  @Override
  public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId,
      String baseURI) {

    var input = new DOMInputImpl();
    var stream = getClass().getResourceAsStream(baseSchemaPath + systemId);
    input.setPublicId(publicId);
    input.setSystemId(systemId);
    input.setBaseURI(baseURI);
    input.setCharacterStream(new InputStreamReader(new BOMInputStream(stream), StandardCharsets.UTF_8));
    return input;
  }
}
