package com.academy.handler;

import com.academy.dto.RoleDto;
import com.academy.model.Role;
import com.academy.service.IRoleService;
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
public class RoleHandler {

    private final IRoleService service;

    @Qualifier("defaultMapper")
    private final ModelMapper mapper;

    private final RequestValidator requestValidator;

    public Mono<ServerResponse> findAll(ServerRequest request) {
        return ServerResponse
                .ok()
                .contentType((MediaType.APPLICATION_JSON))
                .body(service.findAll().map(this::toDto), RoleDto.class);
    }

    public Mono<ServerResponse> findById(ServerRequest request) {
        String id = request.pathVariable("id");

        return service.findById(id)
                .map(this::toDto)
                .flatMap(roleDto -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(roleDto)
                )
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> save(ServerRequest request) {
        Mono<RoleDto> monoRoleDto = request.bodyToMono(RoleDto.class);

        return monoRoleDto
                .flatMap(roleDto -> requestValidator.validate(roleDto, OnCreate.class))
                .flatMap(roleDto -> service.save(toDocument(roleDto)))
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
        Mono<RoleDto> monoRoleDto = request.bodyToMono(RoleDto.class);

        return monoRoleDto
                .flatMap(roleDto -> requestValidator.validate(roleDto, OnUpdate.class))
                .flatMap(roleDto -> service.update(id, toDocument(roleDto)))
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

    private RoleDto toDto(Role role) {
        return mapper.map(role, RoleDto.class);
    }

    private Role toDocument(RoleDto roleDto) {
        return mapper.map(roleDto, Role.class);
    }
}
