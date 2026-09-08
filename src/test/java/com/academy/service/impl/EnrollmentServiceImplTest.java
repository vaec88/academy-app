package com.academy.service.impl;

import com.academy.exception.ModelNotFoundException;
import com.academy.model.Course;
import com.academy.model.EnrolledCourse;
import com.academy.model.EnrolledStudent;
import com.academy.model.Enrollment;
import com.academy.model.Student;
import com.academy.repository.IEnrollmentRepository;
import com.academy.service.ICourseService;
import com.academy.service.IStudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceImplTest {

    private static final String ID = "68c0d2d2d2d2d2d2d2d2d2d2";
    private static final String STUDENT_ID = "68c0f0f0f0f0f0f0f0f0f0f0";
    private static final String COURSE_ID = "68c0e1e1e1e1e1e1e1e1e1e1";
    private static final LocalDateTime ENROLLED_AT = LocalDateTime.of(2026, 3, 10, 8, 0);

    @Mock
    private IEnrollmentRepository enrollmentRepository;

    @Mock
    private IStudentService studentService;

    @Mock
    private ICourseService courseService;

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    @Captor
    private ArgumentCaptor<Enrollment> enrollmentCaptor;

    private Student student;
    private Course course;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        student = new Student(
                STUDENT_ID, "Ana", "Torres", "45871236", 24,
                "ana.torres@academy.com", true, LocalDateTime.of(2026, 1, 15, 9, 30), null);

        course = new Course(
                COURSE_ID, "Reactive Programming", "RXP", true,
                LocalDateTime.of(2026, 1, 20, 10, 0), null);

        enrollment = new Enrollment(
                ID,
                ENROLLED_AT,
                EnrolledStudent.builder().id(STUDENT_ID).build(),
                List.of(EnrolledCourse.builder().id(COURSE_ID).build()));
    }

    @Test
    @DisplayName("findAll emits every enrollment returned by the repository")
    void findAll_shouldEmitAllEnrollments() {
        Enrollment other = new Enrollment(
                "68c0d2d2d2d2d2d2d2d2d2d3", LocalDateTime.of(2026, 4, 2, 9, 15),
                EnrolledStudent.builder().id(STUDENT_ID).build(),
                List.of(EnrolledCourse.builder().id(COURSE_ID).build()));

        when(enrollmentRepository.findAll()).thenReturn(Flux.just(enrollment, other));

        StepVerifier.create(enrollmentService.findAll())
                .expectNext(enrollment)
                .expectNext(other)
                .verifyComplete();

        verify(enrollmentRepository).findAll();
        verifyNoMoreInteractions(enrollmentRepository);
    }

    @Test
    @DisplayName("findAll completes empty when there are no enrollments")
    void findAll_shouldCompleteEmpty() {
        when(enrollmentRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(enrollmentService.findAll())
                .verifyComplete();

        verify(enrollmentRepository).findAll();
    }

    @Test
    @DisplayName("findById emits the matching enrollment")
    void findById_shouldReturnEnrollment() {
        when(enrollmentRepository.findById(ID)).thenReturn(Mono.just(enrollment));

        StepVerifier.create(enrollmentService.findById(ID))
                .expectNext(enrollment)
                .verifyComplete();

        verify(enrollmentRepository).findById(ID);
    }

    @Test
    @DisplayName("findById completes empty when the id does not exist")
    void findById_shouldCompleteEmptyWhenMissing() {
        when(enrollmentRepository.findById(ID)).thenReturn(Mono.empty());

        StepVerifier.create(enrollmentService.findById(ID))
                .verifyComplete();

        verify(enrollmentRepository).findById(ID);
    }

    @Test
    @DisplayName("save embeds the resolved student and courses before persisting")
    void save_shouldPersistEnrollment() {
        when(studentService.findById(STUDENT_ID)).thenReturn(Mono.just(student));
        when(courseService.findAllById(anyIterable())).thenReturn(Flux.just(course));
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(Mono.just(enrollment));

        StepVerifier.create(enrollmentService.save(enrollment))
                .expectNext(enrollment)
                .verifyComplete();

        verify(enrollmentRepository).save(enrollmentCaptor.capture());

        Enrollment saved = enrollmentCaptor.getValue();
        assertEquals(ENROLLED_AT, saved.getEnrollmentDate());
        assertEquals("Ana", saved.getStudent().getFirstName());
        assertEquals("ana.torres@academy.com", saved.getStudent().getEmail());
        assertEquals(1, saved.getCourses().size());
        assertEquals("Reactive Programming", saved.getCourses().getFirst().getName());
    }

    @Test
    @DisplayName("save fails and never persists when the student does not exist")
    void save_shouldFailWhenStudentMissing() {
        when(studentService.findById(STUDENT_ID)).thenReturn(Mono.empty());

        StepVerifier.create(enrollmentService.save(enrollment))
                .verifyError(ModelNotFoundException.class);

        verify(enrollmentRepository, times(0)).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("save fails when one of the requested courses does not exist")
    void save_shouldFailWhenCourseMissing() {
        Enrollment incoming = new Enrollment(
                ID,
                ENROLLED_AT,
                EnrolledStudent.builder().id(STUDENT_ID).build(),
                List.of(
                        EnrolledCourse.builder().id(COURSE_ID).build(),
                        EnrolledCourse.builder().id("68c0e1e1e1e1e1e1e1e1e1e2").build()));

        when(studentService.findById(STUDENT_ID)).thenReturn(Mono.just(student));
        when(courseService.findAllById(anyIterable())).thenReturn(Flux.just(course));

        StepVerifier.create(enrollmentService.save(incoming))
                .verifyError(ModelNotFoundException.class);

        verify(enrollmentRepository, times(0)).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("update resolves and copies only the non-null fields onto the stored enrollment")
    void update_shouldMergeNonNullFieldsAndSave() {
        Course added = new Course(
                "68c0e1e1e1e1e1e1e1e1e1e2", "Spring Security", "SEC", true, null, null);
        Enrollment incoming = new Enrollment(
                null, null, null,
                List.of(
                        EnrolledCourse.builder().id(COURSE_ID).build(),
                        EnrolledCourse.builder().id(added.getId()).build()));

        when(enrollmentRepository.findById(ID)).thenReturn(Mono.just(enrollment));
        when(courseService.findAllById(anyIterable())).thenReturn(Flux.just(course, added));
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(Mono.just(enrollment));

        StepVerifier.create(enrollmentService.update(ID, incoming))
                .expectNext(enrollment)
                .verifyComplete();

        verify(enrollmentRepository).findById(ID);
        verify(enrollmentRepository).save(enrollmentCaptor.capture());

        Enrollment saved = enrollmentCaptor.getValue();
        assertEquals(ID, saved.getId());
        assertEquals(ENROLLED_AT, saved.getEnrollmentDate());
        assertEquals(STUDENT_ID, saved.getStudent().getId());
        assertEquals(2, saved.getCourses().size());
        assertEquals("Spring Security", saved.getCourses().getLast().getName());
    }

    @Test
    @DisplayName("update completes empty and never saves when the id does not exist")
    void update_shouldCompleteEmptyWhenMissing() {
        when(enrollmentRepository.findById(ID)).thenReturn(Mono.empty());

        StepVerifier.create(enrollmentService.update(ID, enrollment))
                .verifyComplete();

        verify(enrollmentRepository).findById(ID);
        verify(enrollmentRepository, times(0)).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("delete emits true when the enrollment exists")
    void delete_shouldReturnTrue() {
        when(enrollmentRepository.findById(ID)).thenReturn(Mono.just(enrollment));
        when(enrollmentRepository.deleteById(ID)).thenReturn(Mono.empty());

        StepVerifier.create(enrollmentService.delete(ID))
                .expectNext(true)
                .verifyComplete();

        verify(enrollmentRepository).findById(ID);
        verify(enrollmentRepository).deleteById(ID);
    }

    @Test
    @DisplayName("delete completes empty when the enrollment does not exist")
    void delete_shouldCompleteEmptyWhenMissing() {
        when(enrollmentRepository.findById(ID)).thenReturn(Mono.empty());

        StepVerifier.create(enrollmentService.delete(ID))
                .verifyComplete();

        verify(enrollmentRepository).findById(ID);
        verify(enrollmentRepository, times(0)).deleteById(ID);
    }
}
