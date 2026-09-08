package com.academy.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RequestValidator {

    private final Validator validator;

    public <T> Mono<T> validate(T document, Class<?>... groups) {
        if (document == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request body"));
        }

        Set<ConstraintViolation<T>> constraints = validator.validate(document, groups);

        if (constraints == null || constraints.isEmpty()) {
            return Mono.just(document);
        }

        String messages = constraints.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(","));

        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, messages));
    }
}
