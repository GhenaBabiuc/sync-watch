package com.example.storageservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI movieStorageOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Movie Storage API")
                        .description("API for storing and managing movies")
                        .version("1.0.0"));
    }
}
