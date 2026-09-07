package com.academy.service.impl;

import com.academy.model.Course;
import com.academy.repository.ICourseRepository;
import com.academy.repository.IGenericRepository;
import com.academy.service.ICourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl extends CrudServiceImpl<Course, String> implements ICourseService {

    private final ICourseRepository courseRepository;

    @Override
    protected IGenericRepository<Course, String> getRepository() {
        return courseRepository;
    }

    @Override
    public Mono<Course> update(String id, Course document) {
        return courseRepository.findById(id)
                .flatMap(courseFound -> {
                    if (document.getName() != null) {
                        courseFound.setName(document.getName());
                    }
                    if (document.getAcronyms() != null) {
                        courseFound.setAcronyms(document.getAcronyms());
                    }
                    if (document.getStatus() != null) {
                        courseFound.setStatus(document.getStatus());
                    }
                    return courseRepository.save(courseFound);
                });
    }

    @Override
    public Flux<Course> findAllById(Iterable<String> ids) {
        return courseRepository.findAllById(ids);
    }
}
