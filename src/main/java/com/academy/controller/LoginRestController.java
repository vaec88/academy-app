package com.academy.controller;

import com.academy.security.AuthRequest;
import com.academy.security.AuthResponse;
import com.academy.security.JwtUtil;
import com.academy.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

//Clase S9
/**
 * The only anonymous endpoint. It is deliberately not under {@code /v1}: it is not a resource.
 */
@RestController
@RequiredArgsConstructor
public class LoginRestController {

    private final IUserService service;

    private final JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * An unknown username produces an empty {@code Mono} and a wrong password is filtered out, so
     * both fall through to the same empty-bodied 401 and cannot be told apart.
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@Valid @RequestBody AuthRequest authRequest) {
        return service.searchByUser(authRequest.getUsername())
                .filterWhen(user -> matches(authRequest.getPassword(), user.getPassword()))
                .map(user ->
                        ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(new AuthResponse(jwtUtil.generateToken(user)))
                )
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * BCrypt verification is CPU intensive by design, so it runs off the event loop.
     */
    private Mono<Boolean> matches(String rawPassword, String encodedPassword) {
        return Mono.fromCallable(() -> passwordEncoder.matches(rawPassword, encodedPassword))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
