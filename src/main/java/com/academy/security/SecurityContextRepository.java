package com.academy.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

//Clase S6
/**
 * Reads the {@code Authorization: Bearer <token>} header and hands the token to the
 * {@link AuthenticationManager}. The API is stateless, so nothing is ever stored.
 */
@Component
@RequiredArgsConstructor
public class SecurityContextRepository implements ServerSecurityContextRepository {

    // Case sensitive on purpose: a lowercase "bearer " prefix is not a valid credential here.
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthenticationManager authenticationManager;

    /**
     * Stateless: there is no session, cookie or store to write to.
     */
    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        return Mono.empty();
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .filter(header -> header.startsWith(BEARER_PREFIX))
                .map(header -> header.substring(BEARER_PREFIX.length()))
                .flatMap(token -> authenticationManager
                        .authenticate(new UsernamePasswordAuthenticationToken(token, token)))
                .map(SecurityContextImpl::new);
    }
}
