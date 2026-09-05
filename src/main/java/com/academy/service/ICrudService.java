package com.academy.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ICrudService<T, K> {

    Flux<T> findAll();

    Mono<T> findById(K id);

    Mono<T> save(T document);

    Mono<T> update(K id, T document);

    Mono<Boolean> delete(K id);
}
