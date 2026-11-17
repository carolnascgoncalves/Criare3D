package br.com.criare3d.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.*;

@Configuration
public class ApiOpenApiConfig {
    @Bean
    public OpenAPI contratoApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Cost3D API")
                                .version("v1")
                                .description(
                                        "API em migração. As páginas atuais continuam funcionando."
                                            + " Valores decimais são strings; datas incluem fuso."
                                            + " Use o token retornado pelo login no botão"
                                            + " Authorize."))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        "bearerAuth",
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")));
    }
}
