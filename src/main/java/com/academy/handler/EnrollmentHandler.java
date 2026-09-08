package com.academy.handler;

import com.academy.dto.EnrollmentDto;
import com.academy.model.Enrollment;
import com.academy.service.IEnrollmentService;
import com.academy.validation.RequestValidator;
import com.academy.validation.groups.OnCreate;
import com.academy.validation.groups.OnUpdate;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class EnrollmentHandler {

    private final IEnrollmentService service;

    @Qualifier("defaultMapper")
    private final ModelMapper mapper;

    private final RequestValidator requestValidator;

    public Mono<ServerResponse> findAll(ServerRequest request) {
        return ServerResponse
                .ok()
                .contentType((MediaType.APPLICATION_JSON))
                .body(service.findAll().map(this::toDto), EnrollmentDto.class);
    }

    public Mono<ServerResponse> findById(ServerRequest request) {
        String id = request.pathVariable("id");

        return service.findById(id)
                .map(this::toDto)
                .flatMap(enrollmentDto -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(enrollmentDto)
                )
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> save(ServerRequest request) {
        Mono<EnrollmentDto> monoEnrollmentDto = request.bodyToMono(EnrollmentDto.class);

        return monoEnrollmentDto
                .flatMap(enrollmentDto -> requestValidator.validate(enrollmentDto, OnCreate.class))
                .flatMap(enrollmentDto -> service.save(toDocument(enrollmentDto)))
                .map(this::toDto)
                .flatMap(savedDto -> ServerResponse
                        .created(request.uri()
                                .resolve(request.path() + "/" + savedDto.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(savedDto)
                );
    }

    public Mono<ServerResponse> update(ServerRequest request) {
        String id = request.pathVariable("id");
        Mono<EnrollmentDto> monoEnrollmentDto = request.bodyToMono(EnrollmentDto.class);

        return monoEnrollmentDto
                .flatMap(enrollmentDto -> requestValidator.validate(enrollmentDto, OnUpdate.class))
                .flatMap(enrollmentDto -> service.update(id, toDocument(enrollmentDto)))
                .map(this::toDto)
                .flatMap(updatedDto -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(updatedDto)
                )
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> delete(ServerRequest request) {
        String id = request.pathVariable("id");

        return service.delete(id)
                .flatMap(_ -> ServerResponse.noContent().build())
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    private EnrollmentDto toDto(Enrollment enrollment) {
        return mapper.map(enrollment, EnrollmentDto.class);
    }

    private Enrollment toDocument(EnrollmentDto enrollmentDto) {
        return mapper.map(enrollmentDto, Enrollment.class);
    }
}
