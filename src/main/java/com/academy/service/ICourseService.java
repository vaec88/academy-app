package com.academy.service;

import com.academy.model.Course;
import reactor.core.publisher.Flux;

public interface ICourseService extends ICrudService<Course, String> {

    Flux<Course> findAllById(Iterable<String> ids);

}
