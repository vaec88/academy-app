package com.academy.handler;

import com.academy.dto.StudentDto;
import com.academy.model.Student;
import com.academy.service.IStudentService;
import com.academy.validation.RequestValidator;
import com.academy.validation.groups.OnCreate;
import com.academy.validation.groups.OnUpdate;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class StudentHandler {

    private final IStudentService service;

    @Qualifier("defaultMapper")
    private final ModelMapper mapper;

    private final RequestValidator requestValidator;

    public Mono<ServerResponse> findAll(ServerRequest request) {
        String sortBy = request.queryParam("sortBy").orElse("createdAt");
        String sortDir = request.queryParam("sortDir").orElse("asc");

        return ServerResponse
                .ok()
                .contentType((MediaType.APPLICATION_JSON))
                .body(service.findAll(Sort.by(Sort.Direction.fromString(sortDir.toLowerCase()), sortBy))
                        .map(this::toDto), StudentDto.class);
    }

    public Mono<ServerResponse> findById(ServerRequest request) {
        String id = request.pathVariable("id");

        return service.findById(id)
                .map(this::toDto)
                .flatMap(studentDto -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(studentDto)
                )
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> save(ServerRequest request) {
        Mono<StudentDto> monoStudentDto = request.bodyToMono(StudentDto.class);

        return monoStudentDto
                .flatMap(studentDto -> requestValidator.validate(studentDto, OnCreate.class))
                .flatMap(studentDto -> service.save(toDocument(studentDto)))
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
        Mono<StudentDto> monoStudentDto = request.bodyToMono(StudentDto.class);

        return monoStudentDto
                .flatMap(studentDto -> requestValidator.validate(studentDto, OnUpdate.class))
                .flatMap(studentDto -> service.update(id, toDocument(studentDto)))
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

    private StudentDto toDto(Student student) {
        return mapper.map(student, StudentDto.class);
    }

    private Student toDocument(StudentDto studentDto) {
        return mapper.map(studentDto, Student.class);
    }
}
