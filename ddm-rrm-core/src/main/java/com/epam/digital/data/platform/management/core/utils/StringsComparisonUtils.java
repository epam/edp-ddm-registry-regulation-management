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

package com.epam.digital.data.platform.management.core.utils;

import org.apache.commons.text.StringEscapeUtils;

/**
 * Strings content comparison methods, excluding Substring.
 */
public class StringsComparisonUtils {

  /**
   * Method for comparing of two strings with ignoring substring from startString to endString
   * including  startString and endString.
   */
  public static boolean compareIgnoringSubstring(
      final String firstString,
      final String secondString,
      final String startSubstring,
      final String endSubstring) {
    String firstStringText = cutSubstring(
        StringEscapeUtils.unescapeJava(firstString), startSubstring, endSubstring)
        .replaceAll("\n", "").replaceAll(" ", "");
    String secondStringText = cutSubstring(
        StringEscapeUtils.unescapeJava(secondString), startSubstring, endSubstring)
        .replaceAll("\n", "").replaceAll(" ", "");
    return firstStringText.equals(secondStringText);
  }

  private static String cutSubstring(String fullString, String startSubstring,
                                     String endSubstring) {
    int beginSubstringPosition = fullString.indexOf(startSubstring);
    if (beginSubstringPosition < 0) {
      return fullString;
    }
    int endSubstringPosition = beginSubstringPosition
        + fullString.substring(beginSubstringPosition)
        .indexOf(endSubstring) + endSubstring.length();

    return fullString.substring(0, beginSubstringPosition)
        + fullString.substring(endSubstringPosition);
  }

}
