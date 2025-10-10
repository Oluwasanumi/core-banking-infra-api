package com.caspercodes.bankingapi.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @io.swagger.v3.oas.annotations.info.Info(
                title = "Banking API",
                version = "1.0.0",
                description = """
                        A secure banking API that provides functionality for:
                        - User authentication and authorization using JWT.
                        - Account management
                        - Transaction processing
                        - Balance inquiries
                        - Fund transfers
                        
                        All endpoints are secured (except auth) and require a valid JWT token for access.
                        """,
                contact = @io.swagger.v3.oas.annotations.info.Contact(
                        name = "Oluwasanumi Balogun",
                        email = "oluwasanumibalogun@gmail.com"
                ),
                license = @io.swagger.v3.oas.annotations.info.License(
                        name = "MIT License",
                        url = "https://opensource.org/license/mit/"
                )),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local server")
        },
        security = {
                @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
        }
)
@SecuritySchemes({
        @io.swagger.v3.oas.annotations.security.SecurityScheme(
                name = "bearerAuth",
                type = io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP,
                scheme = "bearer",
                bearerFormat = "JWT",
                description = "Enter JWT token"
        )
}
)
public class OpenApiConfig {

}