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

package com.epam.digital.data.platform.management.interceptor;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
public abstract class AbstractETagHeaderInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
      Object handler) {
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

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
      ModelAndView modelAndView) {
    if (!request.getMethod().equals("DELETE")) {
      response.setHeader(HttpHeaders.ETAG, getETagFromContent(getContent(request)));
    }
  }

  protected String getETagFromContent(String content) {
    return String.format("\"%s\"", content.hashCode());
  }

  protected Map<String, String> getVariables(HttpServletRequest request) {
    return (Map<String, String>) request.getAttribute(
        HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
  }

  protected boolean validateETag(HttpServletRequest request, HttpServletResponse response,
      String eTag) {
    var content = getContent(request);
    var url = request.getRequestURL();
    if (!getETagFromContent(content).equals(eTag)) {
      log.info("Invalid ETag for path {} version candidate, action will not be performed", url);
      response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
      return false;
    }
    log.debug("Valid ETag for path {}, action will be performed", url);
    return true;
  }

  protected abstract String getContent(HttpServletRequest request);
}
