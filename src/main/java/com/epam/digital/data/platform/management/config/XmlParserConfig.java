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

package com.epam.digital.data.platform.management.config;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class XmlParserConfig {

  @Bean
  public DocumentBuilder documentBuilder() throws ParserConfigurationException {
    var documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities",
        false);
    documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities",
        false);
    documentBuilderFactory.setNamespaceAware(true);
    documentBuilderFactory.setExpandEntityReferences(false);
    return documentBuilderFactory.newDocumentBuilder();
  }
}
