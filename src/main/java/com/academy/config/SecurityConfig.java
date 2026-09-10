package com.academy.config;

import com.academy.exception.GlobalErrorWebExceptionHandler;
import com.academy.security.AuthenticationManager;
import com.academy.security.SecurityContextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

//Clase S7
/**
 * Stateless JWT chain, written by hand rather than pulled in from the resource-server starter so
 * every step stays visible: the {@link SecurityContextRepository} reads the bearer header, the
 * {@link AuthenticationManager} validates the token, and the rules below decide the rest.
 *
 * <p>Both the annotated controllers under {@code /v1} and the functional routes under {@code /v2}
 * are covered — {@code anyExchange()} does not care which one served the path.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GlobalErrorWebExceptionHandler errorHandler;

    private final AuthenticationManager authenticationManager;

    private final SecurityContextRepository securityContextRepository;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                // No cookies and no session, so there is no CSRF vector and no form/basic entry point.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authenticationManager(authenticationManager)
                .securityContextRepository(securityContextRepository)
                .authorizeExchange(exchanges -> exchanges
                        // /login is the only way in; it is also the only anonymous endpoint.
                        .pathMatchers("/login").permitAll()
                        .anyExchange().authenticated())
                // Security translates 401/403 inside its own filter chain, so without this the
                // responses come back with an empty body instead of a CustomErrorResponse.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .build();
    }

    /**
     * Concrete type on purpose: the login flow needs {@code matches}, and every
     * {@code PasswordEncoder} injection point still resolves to this same bean.
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
