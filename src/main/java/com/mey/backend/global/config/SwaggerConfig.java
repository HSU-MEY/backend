package com.mey.backend.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(title = "Mey Backend API",
                version = "v1",
                description = "API 명세서"))
@Configuration
public class SwaggerConfig {

//    @Bean
//    public OpenAPI api() {
//        SecurityScheme apiKey = new SecurityScheme()
//                .type(SecurityScheme.Type.HTTP)
//                .in(SecurityScheme.In.HEADER)
//                .name("Authorization")
//                .scheme("bearer")
//                .bearerFormat("JWT");
//
//        SecurityRequirement securityRequirement = new SecurityRequirement()
//                .addList("Bearer Token");
//
//        return new OpenAPI()
//                .components(new Components().addSecuritySchemes("Bearer Token", apiKey))
//                .addSecurityItem(securityRequirement);
//    }
}
