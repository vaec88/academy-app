package com.academy.service.impl;

import com.academy.model.Role;
import com.academy.repository.IGenericRepository;
import com.academy.repository.IRoleRepository;
import com.academy.service.IRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends CrudServiceImpl<Role, String> implements IRoleService {

    private final IRoleRepository roleRepository;

    @Override
    protected IGenericRepository<Role, String> getRepository() {
        return roleRepository;
    }

    @Override
    public Mono<Role> update(String id, Role document) {
        return roleRepository.findById(id)
                .flatMap(roleFound -> {
                    if (document.getName() != null) {
                        roleFound.setName(document.getName());
                    }
                    if (document.getStatus() != null) {
                        roleFound.setStatus(document.getStatus());
                    }
                    return roleRepository.save(roleFound);
                });
    }
}
