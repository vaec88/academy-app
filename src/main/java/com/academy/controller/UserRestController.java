package com.academy.controller;

import com.academy.dto.UserDto;
import com.academy.model.User;
import com.academy.service.IUserService;
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
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserRestController {

    private final IUserService service;

    @Qualifier("defaultMapper")
    private final ModelMapper mapper;

    @GetMapping
    public Mono<ResponseEntity<Flux<UserDto>>> findAll() {
        Flux<User> users = service.findAll();
        return Mono.just(
                ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(users.map(this::toDto))
        );
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<UserDto>> findById(@PathVariable String id) {
        return service.findById(id)
                .map(user ->
                        ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(toDto(user))
                )
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<ResponseEntity<Void>> save(@Validated(OnCreate.class) @RequestBody UserDto userDto, final ServerHttpRequest request) {
        User user = toDocument(userDto);
        return service.save(user)
                .map(savedUser ->
                        ResponseEntity.created(
                                        request.getURI()
                                                .resolve(request.getPath() + "/" + savedUser.getId())
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .build()
                );
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<UserDto>> update(@PathVariable String id, @Validated(OnUpdate.class) @RequestBody UserDto userDto) {
        User user = toDocument(userDto);
        return service.update(id, user)
                .map(updatedUser ->
                        ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(toDto(updatedUser))
                )
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        return service.delete(id)
                .map(_ -> ResponseEntity.noContent().<Void>build())
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    private UserDto toDto(User user) {
        return mapper.map(user, UserDto.class);
    }

    private User toDocument(UserDto userDto) {
        return mapper.map(userDto, User.class);
    }
}
