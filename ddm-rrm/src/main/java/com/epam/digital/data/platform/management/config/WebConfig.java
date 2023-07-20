/*
 *  Copyright 2022 EPAM Systems.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.epam.digital.data.platform.management.config;

import com.epam.digital.data.platform.management.interceptor.FormETagHeaderInterceptor;
import com.epam.digital.data.platform.management.security.config.SecurityContextResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

  private final HandlerInterceptor livenessProbeStateInterceptor;
  private final FormETagHeaderInterceptor formETagHeaderInterceptor;
  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(new SecurityContextResolver());
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(livenessProbeStateInterceptor);
    registry.addInterceptor(formETagHeaderInterceptor)
        .addPathPatterns(List.of("/versions/candidates/{versionCandidateId}/forms/{formName}",
            "/versions/master/forms/{formName}"));

  }
}
