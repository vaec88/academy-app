package com.academy.controller;

import com.academy.dto.CourseDto;
import com.academy.model.Course;
import com.academy.service.ICourseService;
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
@RequestMapping("/v1/courses")
@RequiredArgsConstructor
public class CourseRestController {

    private final ICourseService service;

    @Qualifier("defaultMapper")
    private final ModelMapper mapper;

    @GetMapping
    public Mono<ResponseEntity<Flux<CourseDto>>> findAll() {
        Flux<Course> courses = service.findAll();
        return Mono.just(
                ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(courses.map(this::toDto))
        );
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<CourseDto>> findById(@PathVariable String id) {
        return service.findById(id)
                .map(course ->
                        ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(toDto(course))
                )
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<ResponseEntity<Void>> save(@Validated(OnCreate.class) @RequestBody CourseDto courseDto, final ServerHttpRequest request) {
        Course course = toDocument(courseDto);
        return service.save(course)
                .map(savedCourse ->
                        ResponseEntity.created(
                                        request.getURI()
                                                .resolve(request.getPath() + "/" + savedCourse.getId())
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .build()
                );
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<CourseDto>> update(@PathVariable String id, @Validated(OnUpdate.class) @RequestBody CourseDto courseDto) {
        Course course = toDocument(courseDto);
        return service.update(id, course)
                .map(updatedCourse ->
                        ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(toDto(updatedCourse))
                )
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        return service.delete(id)
                .map(_ -> ResponseEntity.noContent().<Void>build())
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    private CourseDto toDto(Course course) {
        return mapper.map(course, CourseDto.class);
    }

    private Course toDocument(CourseDto courseDto) {
        return mapper.map(courseDto, Course.class);
    }
}
