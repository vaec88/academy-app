package com.academy.service;

import com.academy.model.Student;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;

public interface IStudentService extends ICrudService<Student, String> {

    Flux<Student> findAll(Sort sort);

}
