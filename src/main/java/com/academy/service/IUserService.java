package com.academy.service;

import com.academy.model.User;
import reactor.core.publisher.Mono;

public interface IUserService extends ICrudService<User, String> {

    //Clase S8
    /**
     * Resolves the login principal: the stored user plus the flat names of its roles.
     *
     * @return empty when no user carries that username — the caller cannot tell an unknown user
     * from a wrong password.
     */
    Mono<com.academy.security.User> searchByUser(String username);

    //Clase S8
    /**
     * Stores the user with a BCrypt-hashed password and no role resolution, which is what the
     * seeding path needs; {@link #save(Object)} is the full CRUD path.
     */
    Mono<User> saveHash(User user);
}
