package com.academy.repository;

import com.academy.model.User;
import reactor.core.publisher.Mono;

//Clase S8
public interface IUserRepository extends IGenericRepository<User, String> {

    /**
     * Derived query: {@code findOneBy...} keeps the return a {@link Mono} even though
     * {@code username} is not declared unique at the collection level.
     */
    Mono<User> findOneByUsername(String username);
}
