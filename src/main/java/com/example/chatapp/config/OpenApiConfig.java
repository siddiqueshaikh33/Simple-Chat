package com.example.chatapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 configuration ensuring Swagger UI uses relative server paths.
 * Resolves browser Mixed Content errors behind reverse proxies (e.g. Railway HTTPS).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("/").description("Default Server (Relative URL)")
                ))
                .info(new Info()
                        .title("Simple Chat Application API")
                        .version("1.0.0")
                        .description("REST API for chat room management, real-time messaging, and participant tracking using Spring Boot 3 and Redis."));
    }
}
