package com.academy.service.impl;

import com.academy.model.Course;
import com.academy.model.Enrollment;
import com.academy.model.Student;
import com.academy.repository.IEnrollmentRepository;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceImplTest {

    private static final String ID = "68c0d2d2d2d2d2d2d2d2d2d2";
    private static final LocalDateTime ENROLLED_AT = LocalDateTime.of(2026, 3, 10, 8, 0);

    @Mock
    private IEnrollmentRepository enrollmentRepository;

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
                "68c0f0f0f0f0f0f0f0f0f0f0", "Ana", "Torres", "45871236", 24,
                "ana.torres@academy.com", true, LocalDateTime.of(2026, 1, 15, 9, 30), null);

        course = new Course(
                "68c0e1e1e1e1e1e1e1e1e1e1", "Reactive Programming", "RXP", true,
                LocalDateTime.of(2026, 1, 20, 10, 0), null);

        enrollment = new Enrollment(ID, ENROLLED_AT, student, List.of(course));
    }

    @Test
    @DisplayName("findAll emits every enrollment returned by the repository")
    void findAll_shouldEmitAllEnrollments() {
        Enrollment other = new Enrollment(
                "68c0d2d2d2d2d2d2d2d2d2d3", LocalDateTime.of(2026, 4, 2, 9, 15),
                student, List.of(course));

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
    @DisplayName("save delegates to the repository with the embedded student and courses")
    void save_shouldPersistEnrollment() {
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(Mono.just(enrollment));

        StepVerifier.create(enrollmentService.save(enrollment))
                .expectNext(enrollment)
                .verifyComplete();

        verify(enrollmentRepository).save(enrollmentCaptor.capture());

        Enrollment saved = enrollmentCaptor.getValue();
        assertEquals(ENROLLED_AT, saved.getEnrollmentDate());
        assertEquals("Ana", saved.getStudent().getFirstName());
        assertEquals(1, saved.getCourses().size());
        assertEquals("RXP", saved.getCourses().getFirst().getAcronyms());
    }

    @Test
    @DisplayName("update copies only the non-null fields onto the stored enrollment")
    void update_shouldMergeNonNullFieldsAndSave() {
        Course added = new Course(
                "68c0e1e1e1e1e1e1e1e1e1e2", "Spring Security", "SEC", true, null, null);
        Enrollment incoming = new Enrollment(null, null, null, List.of(course, added));

        when(enrollmentRepository.findById(ID)).thenReturn(Mono.just(enrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(Mono.just(enrollment));

        StepVerifier.create(enrollmentService.update(ID, incoming))
                .expectNext(enrollment)
                .verifyComplete();

        verify(enrollmentRepository).findById(ID);
        verify(enrollmentRepository).save(enrollmentCaptor.capture());

        Enrollment saved = enrollmentCaptor.getValue();
        assertEquals(ID, saved.getId());
        assertEquals(ENROLLED_AT, saved.getEnrollmentDate());
        assertEquals("Ana", saved.getStudent().getFirstName());
        assertEquals(2, saved.getCourses().size());
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
