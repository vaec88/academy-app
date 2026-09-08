package com.academy.handler;

import com.academy.dto.UserDto;
import com.academy.model.User;
import com.academy.service.IUserService;
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
public class UserHandler {

    private final IUserService service;

    @Qualifier("defaultMapper")
    private final ModelMapper mapper;

    private final RequestValidator requestValidator;

    public Mono<ServerResponse> findAll(ServerRequest request) {
        return ServerResponse
                .ok()
                .contentType((MediaType.APPLICATION_JSON))
                .body(service.findAll().map(this::toDto), UserDto.class);
    }

    public Mono<ServerResponse> findById(ServerRequest request) {
        String id = request.pathVariable("id");

        return service.findById(id)
                .map(this::toDto)
                .flatMap(userDto -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(userDto)
                )
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> save(ServerRequest request) {
        Mono<UserDto> monoUserDto = request.bodyToMono(UserDto.class);

        return monoUserDto
                .flatMap(userDto -> requestValidator.validate(userDto, OnCreate.class))
                .flatMap(userDto -> service.save(toDocument(userDto)))
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
        Mono<UserDto> monoUserDto = request.bodyToMono(UserDto.class);

        return monoUserDto
                .flatMap(userDto -> requestValidator.validate(userDto, OnUpdate.class))
                .flatMap(userDto -> service.update(id, toDocument(userDto)))
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

    private UserDto toDto(User user) {
        return mapper.map(user, UserDto.class);
    }

    private User toDocument(UserDto userDto) {
        return mapper.map(userDto, User.class);
    }
}
