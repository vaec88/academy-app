package com.academy.controller;

import com.academy.dto.RoleDto;
import com.academy.model.Role;
import com.academy.service.IRoleService;
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
@RequestMapping("/v1/roles")
@RequiredArgsConstructor
public class RoleRestController {

    private final IRoleService service;

    @Qualifier("defaultMapper")
    private final ModelMapper mapper;

    @GetMapping
    public Mono<ResponseEntity<Flux<RoleDto>>> findAll() {
        Flux<Role> roles = service.findAll();
        return Mono.just(
                ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(roles.map(this::toDto))
        );
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<RoleDto>> findById(@PathVariable String id) {
        return service.findById(id)
                .map(role ->
                        ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(toDto(role))
                )
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<ResponseEntity<Void>> save(@Validated(OnCreate.class) @RequestBody RoleDto roleDto, final ServerHttpRequest request) {
        Role role = toDocument(roleDto);
        return service.save(role)
                .map(savedRole ->
                        ResponseEntity.created(
                                        request.getURI()
                                                .resolve(request.getPath() + "/" + savedRole.getId())
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .build()
                );
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<RoleDto>> update(@PathVariable String id, @Validated(OnUpdate.class) @RequestBody RoleDto roleDto) {
        Role role = toDocument(roleDto);
        return service.update(id, role)
                .map(updatedRole ->
                        ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(toDto(updatedRole))
                )
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        return service.delete(id)
                .map(_ -> ResponseEntity.noContent().<Void>build())
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    private RoleDto toDto(Role role) {
        return mapper.map(role, RoleDto.class);
    }

    private Role toDocument(RoleDto roleDto) {
        return mapper.map(roleDto, Role.class);
    }
}
