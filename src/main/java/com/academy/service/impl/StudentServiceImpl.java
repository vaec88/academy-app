package com.academy.service.impl;

import com.academy.model.Student;
import com.academy.repository.IGenericRepository;
import com.academy.repository.IStudentRepository;
import com.academy.service.IStudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl extends CrudServiceImpl<Student, String> implements IStudentService {

    private final IStudentRepository studentRepository;

    @Override
    protected IGenericRepository<Student, String> getRepository() {
        return studentRepository;
    }

    @Override
    public Flux<Student> findAll(Sort sort) {
        return studentRepository.findAll(sort);
    }

    @Override
    public Mono<Student> update(String id, Student document) {
        return studentRepository.findById(id)
                .flatMap(studentFound -> {
                    if (document.getFirstName() != null) {
                        studentFound.setFirstName(document.getFirstName());
                    }
                    if (document.getLastName() != null) {
                        studentFound.setLastName(document.getLastName());
                    }
                    if (document.getDni() != null) {
                        studentFound.setDni(document.getDni());
                    }
                    if (document.getAge() != null) {
                        studentFound.setAge(document.getAge());
                    }
                    if (document.getEmail() != null) {
                        studentFound.setEmail(document.getEmail());
                    }
                    if (document.getStatus() != null) {
                        studentFound.setStatus(document.getStatus());
                    }
                    return studentRepository.save(studentFound);
                });
    }
}
