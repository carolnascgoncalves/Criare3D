
package br.com.criare3d.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import br.com.criare3d.service.TenantTemaCache;

@Configuration
public class SecurityConfig {

    private final SenhaObrigatoriaLoginSuccessHandler senhaObrigatoriaLoginSuccessHandler;
    private final TenantTemaCache tenantTemaCache;
    @org.springframework.beans.factory.annotation.Autowired
    private br.com.criare3d.security.LimiteLogin limiteLogin;
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SecurityConfig.class);

    public SecurityConfig(SenhaObrigatoriaLoginSuccessHandler senhaObrigatoriaLoginSuccessHandler,
                          TenantTemaCache tenantTemaCache) {
        this.senhaObrigatoriaLoginSuccessHandler = senhaObrigatoriaLoginSuccessHandler;
        this.tenantTemaCache = tenantTemaCache;
    }

    @Bean
    public org.springframework.security.core.session.SessionRegistry sessionRegistry() {
        return new org.springframework.security.core.session.SessionRegistryImpl();
    }

    @Bean
    public org.springframework.security.web.session.HttpSessionEventPublisher httpSessionEventPublisher() {
        return new org.springframework.security.web.session.HttpSessionEventPublisher();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        return new PasswordEncoder() {
            private boolean cabe(CharSequence senha) {
                return senha != null && senha.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72;
            }
            @Override public String encode(CharSequence senha) {
                if (!cabe(senha)) throw new IllegalArgumentException("A senha deve ocupar no máximo 72 bytes.");
                return bcrypt.encode(senha);
            }
            @Override public boolean matches(CharSequence senha, String hash) {
                return cabe(senha) && bcrypt.matches(senha, hash);
            }
            @Override public boolean upgradeEncoding(String hash) { return bcrypt.upgradeEncoding(hash); }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",
                                "/recuperar-senha",
                                "/recuperar-senha/**",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/vendor/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico"
                        ).permitAll()
                        .requestMatchers("/actuator/**").denyAll()
                        .requestMatchers("/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/clientes", "/clientes/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) -> {
                            limiteLogin.autenticou(request.getParameter("email"));
                            senhaObrigatoriaLoginSuccessHandler.onAuthenticationSuccess(request, response, authentication);
                        })
                        .failureHandler((request, response, exception) -> {
                            if (exception instanceof org.springframework.security.authentication.AuthenticationServiceException) {
                                LOG.error("Autenticação indisponível; tipo={}", exception.getClass().getSimpleName());
                                response.sendRedirect(request.getContextPath() + "/login?indisponivel");
                                return;
                            }
                            String email = request.getParameter("email");
                            String ip = request.getRemoteAddr();
                            limiteLogin.falhou(email, ip);
                            long espera = limiteLogin.esperaSegundos(email, ip);
                            LOG.warn("Falha de autenticação; conta={}, origem={}, tipo={}, espera={}s",
                                    limiteLogin.identificadorConta(email), limiteLogin.identificadorOrigem(ip),
                                    exception.getClass().getSimpleName(), espera);
                            if (espera > 0) {
                                br.com.criare3d.security.AvisoLogin.bloquear(request, response,
                                        limiteLogin.bloqueadoAte(email, ip), espera);
                            } else {
                                response.sendRedirect(request.getContextPath() + "/login?erro");
                            }
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                );

        http.sessionManagement(sessions -> sessions.maximumSessions(-1).sessionRegistry(sessionRegistry()));
        http.addFilterBefore(new br.com.criare3d.security.LimiteLoginFilter(limiteLogin),
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(new PrimeiroAcessoSenhaObrigatoriaFilter(), AuthorizationFilter.class);
        http.addFilterBefore(new TenantObrigatorioFilter(tenantTemaCache), AuthorizationFilter.class);

        return http.build();
    }
}
