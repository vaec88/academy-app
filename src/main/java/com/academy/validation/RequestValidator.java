package com.academy.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * Bean validation for functional endpoints, where {@code @Validated} is not applied:
 * WebFlux.fn only deserializes the body, it never validates it.
 * <p>
 * Violations are raised as a {@link ConstraintViolationException} so that
 * {@code GlobalErrorWebExceptionHandler} can report them per field, the same way it
 * reports the {@code WebExchangeBindException} thrown by the annotated controllers.
 *
 * @param groups validation groups to apply, mirroring {@code @Validated(OnCreate.class)};
 *               none means the {@code Default} group only
 */
@Component
@RequiredArgsConstructor
public class RequestValidator {

    private final Validator validator;

    public <T> Mono<T> validate(T document, Class<?>... groups) {
        if (document == null) {
            return Mono.error(new ServerWebInputException("Request body is required"));
        }

        Set<ConstraintViolation<T>> violations = validator.validate(document, groups);

        return violations.isEmpty()
                ? Mono.just(document)
                : Mono.error(new ConstraintViolationException(violations));
    }
}
