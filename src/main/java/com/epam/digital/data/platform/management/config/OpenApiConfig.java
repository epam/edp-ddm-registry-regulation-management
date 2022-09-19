package com.epam.digital.data.platform.management.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
  @Bean
  public OpenApiCustomiser openApiCustomiser() {
    return openApi -> openApi.setOpenapi("3.0.3");
  }

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(new Info().title("Registry regulations admin-portal")
            .description("This document describes REST API of 'Registry regulations admin-portal'")
            .version("1.0"));
  }
}
