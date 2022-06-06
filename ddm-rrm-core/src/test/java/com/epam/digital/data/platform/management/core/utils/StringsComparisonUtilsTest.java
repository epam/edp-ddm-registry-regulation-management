package com.epam.digital.data.platform.management.core.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class StringsComparisonUtilsTest {

  private final String FORM_CONTENT =
      "test content for check Compare Ignoring Substring Method Test ";

  @Test
  void checkCompareIgnoringSubstringMethodTest() {

    String newFormContent = FORM_CONTENT.replaceFirst(
        " for",
        " for123");

    Assertions.assertThat(StringsComparisonUtils.compareIgnoringSubstring(
        FORM_CONTENT,
        newFormContent,
        "for",
        " ")).isTrue();

    newFormContent = FORM_CONTENT.replaceFirst(
        "Compare",
        "Compare1");

    Assertions.assertThat(StringsComparisonUtils.compareIgnoringSubstring(
        FORM_CONTENT,
        newFormContent,
        "fot",
        " ")).isFalse();
  }
}