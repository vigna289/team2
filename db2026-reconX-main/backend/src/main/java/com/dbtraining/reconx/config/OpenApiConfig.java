package com.dbtraining.reconx.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.*;

/**
 * Swagger/OpenAPI metadata plus a bearerAuth (HTTP/bearer/JWT) security
 * scheme so Swagger UI shows an Authorize button ready for Day 5's JWT
 * work. Splits endpoints into a public group (trades, recon) and an admin
 * group (admin endpoints, actuator) so auditors never see admin-only
 * routes on the exported public docs.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI reconxOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("ReconX API")
                .description("Trade reconciliation platform — DB TDI 2026")
                .version("v1.0.0")
                .contact(new Contact()
                    .name("ReconX Team")
                    .email("reconx-team@dbtraining.com")))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("public")
            .pathsToMatch("/v1/trades/**", "/v1/recon/**")
            .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
            .group("admin")
            .pathsToMatch("/v1/admin/**", "/actuator/**")
            .build();
    }
}
