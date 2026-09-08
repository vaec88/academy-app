package com.academy.service;

import com.academy.model.Role;
import reactor.core.publisher.Flux;

public interface IRoleService extends ICrudService<Role, String> {

    Flux<Role> findAllById(Iterable<String> ids);

}
