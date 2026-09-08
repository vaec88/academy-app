package com.academy.service.impl;

import com.academy.exception.ModelNotFoundException;
import com.academy.model.AssignedRole;
import com.academy.model.User;
import com.academy.repository.IGenericRepository;
import com.academy.repository.IUserRepository;
import com.academy.service.IRoleService;
import com.academy.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends CrudServiceImpl<User, String> implements IUserService {

    private final IUserRepository userRepository;

    private final IRoleService roleService;

    private final PasswordEncoder passwordEncoder;

    @Override
    protected IGenericRepository<User, String> getRepository() {
        return userRepository;
    }

    @Override
    public Mono<User> save(User document) {
        return resolveRoles(document.getRoles())
                .flatMap(assignedRoles -> encodePassword(document.getPassword())
                        .flatMap(encodedPassword -> {
                            document.setRoles(assignedRoles);
                            document.setPassword(encodedPassword);
                            return userRepository.save(document);
                        }));
    }

    @Override
    public Mono<User> update(String id, User document) {
        return userRepository.findById(id)
                .flatMap(userFound -> {
                    if (document.getUsername() != null) {
                        userFound.setUsername(document.getUsername());
                    }
                    if (document.getStatus() != null) {
                        userFound.setStatus(document.getStatus());
                    }
                    Mono<String> passwordResolution = document.getPassword() == null
                            ? Mono.empty()
                            : encodePassword(document.getPassword());
                    Mono<List<AssignedRole>> rolesResolution = document.getRoles() == null
                            ? Mono.empty()
                            : resolveRoles(document.getRoles());

                    return passwordResolution.doOnNext(userFound::setPassword)
                            .then(rolesResolution.doOnNext(userFound::setRoles))
                            .then(Mono.defer(() -> userRepository.save(userFound)));
                });
    }

    private Mono<List<AssignedRole>> resolveRoles(List<AssignedRole> assignedRoles) {
        Set<String> roleIds = assignedRoles.stream()
                .map(AssignedRole::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return roleService.findAllById(roleIds)
                .map(role -> AssignedRole.builder()
                        .id(role.getId())
                        .name(role.getName())
                        .build())
                .collectList()
                .flatMap(foundAssignedRoles -> {
                    if (foundAssignedRoles.size() != roleIds.size()) {
                        Set<String> missingIds = new LinkedHashSet<>(roleIds);
                        foundAssignedRoles.forEach(assignedRole -> missingIds.remove(assignedRole.getId()));
                        return Mono.error(new ModelNotFoundException("Roles with ids: " + missingIds + " not found"));
                    }
                    return Mono.just(foundAssignedRoles);
                });
    }

    /**
     * BCrypt hashing is CPU intensive, so it runs off the event loop.
     */
    private Mono<String> encodePassword(String rawPassword) {
        return Mono.fromCallable(() -> passwordEncoder.encode(rawPassword))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
