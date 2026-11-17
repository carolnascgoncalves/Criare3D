package br.com.criare3d.config;

import br.com.criare3d.api.auth.*;

import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;

@Configuration
public class ApiSecurityConfig {
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurity(
            HttpSecurity http, ApiJwtService jwt, ApiContas contas, ApiAuthService auth, ApiWebOrigins web)
            throws Exception {
        // As rotas /api/* antigas pertencem ao site e conservam cookie e CSRF.
        return http.securityMatcher(
                        "/api/v1",
                        "/api/v1/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html")
                .cors(cors -> cors.configurationSource(web.cors()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(
                        c -> c.securityContextRepository(new NullSecurityContextRepository()))
                .requestCache(c -> c.disable())
                .authorizeHttpRequests(
                        a ->
                                a.requestMatchers(
                                                "/api/v1/auth/login",
                                                "/api/v1/auth/refresh",
                                                "/api/v1/auth/web/login",
                                                "/api/v1/auth/web/refresh",
                                                "/api/v1/auth/web/logout",
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        e ->
                                e.authenticationEntryPoint(
                                                (r, s, ex) ->
                                                        ApiRespostaSeguranca.enviar(
                                                                s,
                                                                HttpStatus.UNAUTHORIZED,
                                                                "autenticacao_necessaria",
                                                                "Informe um token de acesso"
                                                                    + " válido."))
                                        .accessDeniedHandler(
                                                (r, s, ex) ->
                                                        ApiRespostaSeguranca.enviar(
                                                                s,
                                                                HttpStatus.FORBIDDEN,
                                                                "acesso_negado",
                                                                "Acesso não permitido.")))
                .headers(h -> h.cacheControl(c -> {}))
                .addFilterBefore(
                        new ApiBearerFilter(jwt, contas, auth),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
