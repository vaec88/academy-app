package com.academy.service.impl;

import java.lang.reflect.Method;

import com.academy.repository.IGenericRepository;
import com.academy.service.ICrudService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public abstract class CrudServiceImpl<T, K> implements ICrudService<T, K> {

    protected abstract IGenericRepository<T, K> getRepository();

    @Override
    public Flux<T> findAll() {
        return getRepository().findAll();
    }

    @Override
    public Mono<T> findById(K id) {
        return getRepository().findById(id);
    }

    @Override
    public Mono<T> save(T document) {
        return getRepository().save(document);
    }

    @Override
    public Mono<T> update(K id, T document) {
        return getRepository().findById(id)
                .flatMap(_ -> {
                    try {
                        Method method = document.getClass().getMethod("setId", id.getClass());
                        method.invoke(document, id);
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                    return getRepository().save(document);
                });
    }

    @Override
    public Mono<Boolean> delete(K id) {
        return getRepository().findById(id)
                .flatMap(_ -> getRepository().deleteById(id).thenReturn(true));
    }
}
