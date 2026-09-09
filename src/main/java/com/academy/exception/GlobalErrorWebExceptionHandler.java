package com.academy.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class GlobalErrorWebExceptionHandler implements WebExceptionHandler {

    //private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);

    private static final Comparator<CustomErrorResponse.ValidationError> BY_FIELD_THEN_MESSAGE =
            Comparator.comparing(CustomErrorResponse.ValidationError::field)
                    .thenComparing(CustomErrorResponse.ValidationError::message);

    private final ServerCodecConfigurer codecConfigurer;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // A streaming body (findAll) may already be on the wire; the status line and part of the
        // payload are gone, so replacing them here would fail. Let the error propagate instead.
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        String path = exchange.getRequest().getPath().value();

        CustomErrorResponse errorResponse = switch (ex) {
            // Annotated controllers: @Validated(...) @RequestBody
            case WebExchangeBindException bindEx -> validationFailed(path, bindEx.getAllErrors().stream()
                    .map(GlobalErrorWebExceptionHandler::toValidationError)
                    .toList());

            // Functional handlers: RequestValidator
            case ConstraintViolationException violationEx ->
                    validationFailed(path, violationEx.getConstraintViolations().stream()
                            .map(GlobalErrorWebExceptionHandler::toValidationError)
                            .toList());

            // Unreadable or missing body, wrong path variable type, ...
            case ServerWebInputException inputEx -> new CustomErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    messageOrDefault(inputEx.getReason(), "Malformed or missing request body"),
                    path
            );

            case ModelNotFoundException notFoundEx -> new CustomErrorResponse(
                    HttpStatus.NOT_FOUND.value(),
                    messageOrDefault(notFoundEx.getMessage(), "Resource not found"),
                    path
            );

            case ResponseStatusException statusEx -> new CustomErrorResponse(
                    statusEx.getStatusCode().value(),
                    messageOrDefault(statusEx.getReason(), reasonPhrase(statusEx.getStatusCode().value())),
                    path
            );

            default -> new CustomErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal server error",
                    path
            );
        };

        logError(exchange, errorResponse, ex);

        return ServerResponse.status(errorResponse.status())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(errorResponse)
                .flatMap(response -> response.writeTo(exchange, new ServerResponse.Context() {
                    @Override
                    public List<HttpMessageWriter<?>> messageWriters() {
                        return codecConfigurer.getWriters();
                    }

                    @Override
                    public List<ViewResolver> viewResolvers() {
                        return Collections.emptyList();
                    }
                }));
    }

    private static CustomErrorResponse validationFailed(String path, List<CustomErrorResponse.ValidationError> errors) {
        List<CustomErrorResponse.ValidationError> sorted = errors.stream()
                .sorted(BY_FIELD_THEN_MESSAGE)
                .toList();

        String summary = sorted.stream()
                .map(error -> error.field() + ": " + error.message())
                .collect(Collectors.joining(", "));

        return new CustomErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                messageOrDefault(summary, "Validation failed"),
                path,
                sorted.isEmpty() ? null : sorted
        );
    }

    private static CustomErrorResponse.ValidationError toValidationError(ObjectError error) {
        String field = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
        return new CustomErrorResponse.ValidationError(field, messageOrDefault(error.getDefaultMessage(), "is invalid"));
    }

    private static CustomErrorResponse.ValidationError toValidationError(ConstraintViolation<?> violation) {
        return new CustomErrorResponse.ValidationError(
                violation.getPropertyPath().toString(),
                messageOrDefault(violation.getMessage(), "is invalid")
        );
    }

    private static String reasonPhrase(int status) {
        HttpStatus resolved = HttpStatus.resolve(status);
        return resolved != null ? resolved.getReasonPhrase() : "Unexpected error";
    }

    private static String messageOrDefault(String message, String fallback) {
        return message == null || message.isBlank() ? fallback : message;
    }

    private static void logError(ServerWebExchange exchange, CustomErrorResponse errorResponse, Throwable ex) {
        String request = exchange.getRequest().getMethod() + " " + errorResponse.path();

        if (errorResponse.status() >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            log.error("{} failed with {}", request, errorResponse.status(), ex);
        } else {
            log.debug("{} rejected with {}: {}", request, errorResponse.status(), errorResponse.message());
        }
    }
}
