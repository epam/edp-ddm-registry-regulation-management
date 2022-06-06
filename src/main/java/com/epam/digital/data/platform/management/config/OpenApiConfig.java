package com.epam.digital.data.platform.management.config;

import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
  @Bean
  public OpenApiCustomiser openApiCustomiser() {
    return openApi -> openApi.setOpenapi("3.0.3");
  }
}
