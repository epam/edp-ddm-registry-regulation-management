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

package com.epam.digital.data.platform.management.core.context;

import com.epam.digital.data.platform.management.core.config.VersionContextConfig;
import java.util.Map;
import net.bytebuddy.utility.RandomString;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    TestVersionBeanFactory.class,
    RecreateTestVersionBeanFactory.class,
    VersionContextConfig.class
})
@DisplayName("com.epam.digital.data.platform.management.core.context.VersionContext")
class VersionContextTest {

  @Autowired
  VersionContext versionContext;

  @Test
  @DisplayName("should return the same object for same parameters if recreate - false")
  void testGetBean_sameBeanForSameVersion() {
    var versionId = "master";

    var resultBean = versionContext.getBean(versionId, String.class);
    Assertions.assertThat(resultBean)
        .isNotNull()
        .contains(versionId);

    var storedBean = versionContext.getBean(versionId, String.class);
    Assertions.assertThat(storedBean).isSameAs(resultBean);
  }

  @Test
  @DisplayName("should return different objects for same parameters if recreate - true")
  void testGetBean_differentBeanForSameVersion() {
    var versionId = "master";

    var resultBean = versionContext.getBean(versionId, Integer.class);
    Assertions.assertThat(resultBean)
        .isNotNull();

    var storedBean = versionContext.getBean(versionId, Integer.class);
    Assertions.assertThat(storedBean).isNotSameAs(resultBean);
  }

  @Test
  @DisplayName("should return different objects for different versions")
  void testGetBean_differentBeansForDifferentVersions() {
    var version1 = "version1";
    var version2 = "version2";

    var resultVersion1Bean = versionContext.getBean(version1, String.class);
    Assertions.assertThat(resultVersion1Bean)
        .isNotNull()
        .contains(version1);

    var storedBean = versionContext.getBean(version2, String.class);
    Assertions.assertThat(storedBean).isNotEqualTo(version2);
  }

  @Test
  @DisplayName("should throw IllegalArgumentException if there are no VersionBeanFactories for class")
  void testGetBean_illegalArgument() {
    var version = RandomString.make();

    Assertions.assertThatThrownBy(() -> versionContext.getBean(version, Object.class))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("No VersionBeanFactory is registered for bean type %s", Object.class);
  }


  @Test
  @DisplayName("should delete context for the specific version")
  @SuppressWarnings("unchecked")
  void destroyContext() {
    var version1 = "version1";
    var version2 = "version2";
    versionContext.getBean(version1, String.class);
    versionContext.getBean(version2, String.class);

    var contextMap = (Map<String, ?>) ReflectionTestUtils.getField(versionContext, "contextMap");
    Assertions.assertThat(contextMap)
        .containsKeys(version1, version2);

    versionContext.destroyContext(version1);
    Assertions.assertThat(contextMap)
        .doesNotContainKey(version1)
        .containsKey(version2);
  }
}
