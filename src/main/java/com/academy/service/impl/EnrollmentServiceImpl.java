package com.academy.service.impl;

import com.academy.exception.ModelNotFoundException;
import com.academy.model.EnrolledCourse;
import com.academy.model.EnrolledStudent;
import com.academy.model.Enrollment;
import com.academy.repository.IEnrollmentRepository;
import com.academy.repository.IGenericRepository;
import com.academy.service.ICourseService;
import com.academy.service.IEnrollmentService;
import com.academy.service.IStudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl extends CrudServiceImpl<Enrollment, String> implements IEnrollmentService {

    private final IEnrollmentRepository enrollmentRepository;

    private final IStudentService studentService;

    private final ICourseService courseService;

    @Override
    protected IGenericRepository<Enrollment, String> getRepository() {
        return enrollmentRepository;
    }

    @Override
    public Mono<Enrollment> save(Enrollment document) {
        String studentId = document.getStudent().getId();
        Set<String> courseIds = document.getCourses().stream()
                .map(EnrolledCourse::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return studentService.findById(studentId)
                .switchIfEmpty(Mono.error(new ModelNotFoundException("Student with id: " + studentId + " not found")))
                .map(student -> EnrolledStudent.builder()
                        .id(student.getId())
                        .firstName(student.getFirstName())
                        .lastName(student.getLastName())
                        .email(student.getEmail())
                        .build())
                .flatMap(enrolledStudent -> courseService.findAllById(courseIds)
                        .map(course -> EnrolledCourse.builder()
                                .id(course.getId())
                                .name(course.getName())
                                .build())
                        .collectList()
                        .flatMap(enrolledCourses -> {
                            if (enrolledCourses.size() != courseIds.size()) {
                                Set<String> missingIds = new LinkedHashSet<>(courseIds);
                                enrolledCourses.forEach(enrolledCourse -> missingIds.remove(enrolledCourse.getId()));
                                return Mono.error(new ModelNotFoundException("Courses with ids: " + missingIds + " not found"));
                            }
                            document.setStudent(enrolledStudent);
                            document.setCourses(enrolledCourses);
                            return enrollmentRepository.save(document);
                        }));
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
