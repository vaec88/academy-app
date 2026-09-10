package com.academy.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

//Clase S5
/**
 * Turns the raw token carried in {@link Authentication#getCredentials()} into an authenticated
 * {@link Authentication} whose authorities are the {@code roles} claim.
 *
 * <p>An invalid token yields {@link Mono#empty()} rather than an error: the chain then has no
 * security context, {@code anyExchange().authenticated()} rejects the exchange, and the
 * {@code GlobalErrorWebExceptionHandler} entry point writes the 401.
 */
@Component
@RequiredArgsConstructor
public class AuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtUtil jwtUtil;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        return Mono.justOrEmpty(authentication.getCredentials())
                .map(String::valueOf)
                // Parsing is CPU-only (HMAC over a short string), so it stays on the event loop.
                .filter(jwtUtil::validateToken)
                .map(this::toAuthentication);
    }

    private Authentication toAuthentication(String token) {
        return new UsernamePasswordAuthenticationToken(
                jwtUtil.getUsernameFromToken(token),
                null,
                jwtUtil.getRolesFromToken(token).stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList()
        );
    }
}
