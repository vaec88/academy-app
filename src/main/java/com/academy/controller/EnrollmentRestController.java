package com.academy.controller;

import com.academy.dto.EnrollmentDto;
import com.academy.model.Enrollment;
import com.academy.service.IEnrollmentService;
import com.academy.validation.groups.OnCreate;
import com.academy.validation.groups.OnUpdate;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentRestController {

    private final IEnrollmentService service;

    @Qualifier("defaultMapper")
    private final ModelMapper mapper;

    @GetMapping
    public Mono<ResponseEntity<Flux<EnrollmentDto>>> findAll() {
        Flux<Enrollment> enrollments = service.findAll();
        return Mono.just(
                ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(enrollments.map(this::toDto))
        );
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<EnrollmentDto>> findById(@PathVariable String id) {
        return service.findById(id)
                .map(enrollment ->
                        ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(toDto(enrollment))
                )
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<ResponseEntity<Void>> save(@Validated(OnCreate.class) @RequestBody EnrollmentDto enrollmentDto, final ServerHttpRequest request) {
        Enrollment enrollment = toDocument(enrollmentDto);
        return service.save(enrollment)
                .map(savedEnrollment ->
                        ResponseEntity.created(
                                        request.getURI()
                                                .resolve(request.getPath() + "/" + savedEnrollment.getId())
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .build()
                );
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<EnrollmentDto>> update(@PathVariable String id, @Validated(OnUpdate.class) @RequestBody EnrollmentDto enrollmentDto) {
        Enrollment enrollment = toDocument(enrollmentDto);
        return service.update(id, enrollment)
                .map(updatedEnrollment ->
                        ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(toDto(updatedEnrollment))
                )
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        return service.delete(id)
                .map(_ -> ResponseEntity.noContent().<Void>build())
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    private EnrollmentDto toDto(Enrollment enrollment) {
        return mapper.map(enrollment, EnrollmentDto.class);
    }

    private Enrollment toDocument(EnrollmentDto enrollmentDto) {
        return mapper.map(enrollmentDto, Enrollment.class);
    }
}
