package com.academy.controller;

import com.academy.dto.StudentDto;
import com.academy.model.Student;
import com.academy.service.IStudentService;
import com.academy.validation.groups.OnCreate;
import com.academy.validation.groups.OnUpdate;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/students")
@RequiredArgsConstructor
public class StudentRestController {

    private final IStudentService service;

    @Qualifier("defaultMapper")
    private final ModelMapper mapper;

    @GetMapping
    public Mono<ResponseEntity<Flux<StudentDto>>> findAll(@RequestParam(defaultValue = "createdAt") String sortBy,
                                                          @RequestParam(defaultValue = "asc") String sortDir) {
        Flux<Student> students = service.findAll(Sort.by(Sort.Direction.fromString(sortDir.toLowerCase()), sortBy));
        return Mono.just(
                ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(students.map(this::toDto))
        );
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<StudentDto>> findById(@PathVariable String id) {
        return service.findById(id)
                .map(student ->
                        ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(toDto(student))
                )
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<ResponseEntity<Void>> save(@Validated(OnCreate.class) @RequestBody StudentDto studentDto, final ServerHttpRequest request) {
        Student student = toDocument(studentDto);
        return service.save(student)
                .map(savedStudent ->
                        ResponseEntity.created(
                                        request.getURI()
                                                .resolve(request.getPath() + "/" + savedStudent.getId())
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .build()
                );
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<StudentDto>> update(@PathVariable String id, @Validated(OnUpdate.class) @RequestBody StudentDto studentDto) {
        Student student = toDocument(studentDto);
        return service.update(id, student)
                .map(updatedStudent ->
                        ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(toDto(updatedStudent))
                )
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        return service.delete(id)
                .map(_ -> ResponseEntity.noContent().<Void>build())
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    private StudentDto toDto(Student student) {
        return mapper.map(student, StudentDto.class);
    }

    private Student toDocument(StudentDto studentDto) {
        return mapper.map(studentDto, Student.class);
    }
}
