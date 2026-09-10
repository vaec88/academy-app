package com.academy.security;

import com.fasterxml.jackson.annotation.JsonProperty;

//Clase S3
/**
 * The {@code POST /login} 200 body. The wire name is {@code access_token}, not {@code token}.
 */
public record AuthResponse(@JsonProperty("access_token") String token) {
}
