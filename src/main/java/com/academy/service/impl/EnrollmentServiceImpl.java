package com.academy.service.impl;

import com.academy.model.Enrollment;
import com.academy.repository.IEnrollmentRepository;
import com.academy.repository.IGenericRepository;
import com.academy.service.IEnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl extends CrudServiceImpl<Enrollment, String> implements IEnrollmentService {

    private final IEnrollmentRepository enrollmentRepository;

    @Override
    protected IGenericRepository<Enrollment, String> getRepository() {
        return enrollmentRepository;
    }

    @Override
    public Mono<Enrollment> update(String id, Enrollment document) {
        return enrollmentRepository.findById(id)
                .flatMap(enrollmentFound -> {
                    if (document.getEnrollmentDate() != null) {
                        enrollmentFound.setEnrollmentDate(document.getEnrollmentDate());
                    }
                    if (document.getStudent() != null) {
                        enrollmentFound.setStudent(document.getStudent());
                    }
                    if (document.getCourses() != null) {
                        enrollmentFound.setCourses(document.getCourses());
                    }
                    return enrollmentRepository.save(enrollmentFound);
                });
    }
}
