
package br.com.criare3d.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
public class SecurityConfig {

    private final SenhaObrigatoriaLoginSuccessHandler senhaObrigatoriaLoginSuccessHandler;

    public SecurityConfig(SenhaObrigatoriaLoginSuccessHandler senhaObrigatoriaLoginSuccessHandler) {
        this.senhaObrigatoriaLoginSuccessHandler = senhaObrigatoriaLoginSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico"
                        ).permitAll()
                        .requestMatchers("/usuarios/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(senhaObrigatoriaLoginSuccessHandler)
                        .failureUrl("/login?erro")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                );

        http.addFilterBefore(new PrimeiroAcessoSenhaObrigatoriaFilter(), AuthorizationFilter.class);
        http.addFilterBefore(new TenantObrigatorioFilter(), AuthorizationFilter.class);

        return http.build();
    }
}
