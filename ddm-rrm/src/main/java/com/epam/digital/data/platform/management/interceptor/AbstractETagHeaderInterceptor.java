/*
 * Copyright 2023 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.management.interceptor;

import com.epam.digital.data.platform.management.core.utils.ETagUtils;
import com.epam.digital.data.platform.management.exception.ProcessNotFoundException;
import com.epam.digital.data.platform.management.forms.exception.FormNotFoundException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Slf4j
public abstract class AbstractETagHeaderInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(@NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response, @NonNull Object handler) {
    var method = request.getMethod();
    var eTag = request.getHeader("If-Match");
    if (!method.equals("PUT") && !method.equals("DELETE")) {
      return true;
    }
    if (eTag == null || ("*").equals(eTag)) {
      log.debug("ETag is null or *, action will be performed");
      return true;
    }
    log.debug("ETag is not null, validating");
    return validateETag(request, response, eTag);
  }

  @SuppressWarnings("unchecked")
  protected Map<String, String> getVariables(HttpServletRequest request) {
    return (Map<String, String>) request.getAttribute(
        HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
  }

  protected boolean validateETag(HttpServletRequest request, HttpServletResponse response,
      String eTag) {
    String content;
    var url = request.getRequestURL();
    try {
      content = getContent(request);
    } catch (FormNotFoundException | ProcessNotFoundException exception) {
      log.warn("ETag validation for path {} failed, content not found", url);
      response.setStatus(HttpServletResponse.SC_CONFLICT);
      return false;
    }
    if (!ETagUtils.getETagFromContent(content).equals(eTag)) {
      log.warn("Invalid ETag for path {} version candidate, action will not be performed", url);
      response.setStatus(HttpServletResponse.SC_CONFLICT);
      return false;
    }
    log.debug("Valid ETag for path {}, action will be performed", url);
    return true;
  }

  protected abstract String getContent(HttpServletRequest request);
}
