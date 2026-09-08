package com.academy.handler;

import com.academy.dto.CourseDto;
import com.academy.model.Course;
import com.academy.service.ICourseService;
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
public class CourseHandler {

    private final ICourseService service;

    @Qualifier("defaultMapper")
    private final ModelMapper mapper;

    private final RequestValidator requestValidator;

    public Mono<ServerResponse> findAll(ServerRequest request) {
        return ServerResponse
                .ok()
                .contentType((MediaType.APPLICATION_JSON))
                .body(service.findAll().map(this::toDto), CourseDto.class);
    }

    public Mono<ServerResponse> findById(ServerRequest request) {
        String id = request.pathVariable("id");

        return service.findById(id)
                .map(this::toDto)
                .flatMap(courseDto -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(courseDto)
                )
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> save(ServerRequest request) {
        Mono<CourseDto> monoCourseDto = request.bodyToMono(CourseDto.class);

        return monoCourseDto
                .flatMap(courseDto -> requestValidator.validate(courseDto, OnCreate.class))
                .flatMap(courseDto -> service.save(toDocument(courseDto)))
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
        Mono<CourseDto> monoCourseDto = request.bodyToMono(CourseDto.class);

        return monoCourseDto
                .flatMap(courseDto -> requestValidator.validate(courseDto, OnUpdate.class))
                .flatMap(courseDto -> service.update(id, toDocument(courseDto)))
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

    private CourseDto toDto(Course course) {
        return mapper.map(course, CourseDto.class);
    }

    private Course toDocument(CourseDto courseDto) {
        return mapper.map(courseDto, Course.class);
    }
}
