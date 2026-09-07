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
import java.util.List;
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
        return resolveStudent(document.getStudent())
                .flatMap(enrolledStudent -> resolveCourses(document.getCourses())
                        .flatMap(enrolledCourses -> {
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
                    Mono<EnrolledStudent> studentResolution = document.getStudent() == null
                            ? Mono.empty()
                            : resolveStudent(document.getStudent());
                    Mono<List<EnrolledCourse>> coursesResolution = document.getCourses() == null
                            ? Mono.empty()
                            : resolveCourses(document.getCourses());

                    return studentResolution.doOnNext(enrollmentFound::setStudent)
                            .then(coursesResolution.doOnNext(enrollmentFound::setCourses))
                            .then(Mono.defer(() -> enrollmentRepository.save(enrollmentFound)));
                });
    }

    private Mono<EnrolledStudent> resolveStudent(EnrolledStudent enrolledStudent) {
        String studentId = enrolledStudent.getId();
        return studentService.findById(studentId)
                .switchIfEmpty(Mono.error(new ModelNotFoundException("Student with id: " + studentId + " not found")))
                .map(foundStudent -> EnrolledStudent.builder()
                        .id(foundStudent.getId())
                        .firstName(foundStudent.getFirstName())
                        .lastName(foundStudent.getLastName())
                        .email(foundStudent.getEmail())
                        .build());
    }

    private Mono<List<EnrolledCourse>> resolveCourses(List<EnrolledCourse> enrolledCourses) {
        Set<String> courseIds = enrolledCourses.stream()
                .map(EnrolledCourse::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return courseService.findAllById(courseIds)
                .map(course -> EnrolledCourse.builder()
                        .id(course.getId())
                        .name(course.getName())
                        .build())
                .collectList()
                .flatMap(foundEnrolledCourses -> {
                    if (foundEnrolledCourses.size() != courseIds.size()) {
                        Set<String> missingIds = new LinkedHashSet<>(courseIds);
                        foundEnrolledCourses.forEach(enrolledCourse -> missingIds.remove(enrolledCourse.getId()));
                        return Mono.error(new ModelNotFoundException("Courses with ids: " + missingIds + " not found"));
                    }
                    return Mono.just(foundEnrolledCourses);
                });
    }
}
