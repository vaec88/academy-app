package com.academy.service.impl;

import com.academy.model.Course;
import com.academy.repository.ICourseRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    private static final String ID = "68c0e1e1e1e1e1e1e1e1e1e1";

    @Mock
    private ICourseRepository courseRepository;

    @InjectMocks
    private CourseServiceImpl courseService;

    @Captor
    private ArgumentCaptor<Course> courseCaptor;

    private Course course;

    @BeforeEach
    void setUp() {
        course = new Course(
                ID,
                "Reactive Programming",
                "RXP",
                true,
                LocalDateTime.of(2026, 1, 15, 9, 30),
                null
        );
    }

    @Test
    @DisplayName("findAll emits every course returned by the repository")
    void findAll_shouldEmitAllCourses() {
        Course other = new Course(
                "68c0e1e1e1e1e1e1e1e1e1e2", "Spring Security", "SEC", true,
                LocalDateTime.of(2026, 2, 1, 8, 0), null);

        when(courseRepository.findAll()).thenReturn(Flux.just(course, other));

        StepVerifier.create(courseService.findAll())
                .expectNext(course)
                .expectNext(other)
                .verifyComplete();

        verify(courseRepository).findAll();
        verifyNoMoreInteractions(courseRepository);
    }

    @Test
    @DisplayName("findAll completes empty when there are no courses")
    void findAll_shouldCompleteEmpty() {
        when(courseRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(courseService.findAll())
                .verifyComplete();

        verify(courseRepository).findAll();
    }

    @Test
    @DisplayName("findById emits the matching course")
    void findById_shouldReturnCourse() {
        when(courseRepository.findById(ID)).thenReturn(Mono.just(course));

        StepVerifier.create(courseService.findById(ID))
                .expectNext(course)
                .verifyComplete();

        verify(courseRepository).findById(ID);
    }

    @Test
    @DisplayName("findById completes empty when the id does not exist")
    void findById_shouldCompleteEmptyWhenMissing() {
        when(courseRepository.findById(ID)).thenReturn(Mono.empty());

        StepVerifier.create(courseService.findById(ID))
                .verifyComplete();

        verify(courseRepository).findById(ID);
    }

    @Test
    @DisplayName("save delegates to the repository and emits the persisted course")
    void save_shouldPersistCourse() {
        when(courseRepository.save(any(Course.class))).thenReturn(Mono.just(course));

        StepVerifier.create(courseService.save(course))
                .expectNext(course)
                .verifyComplete();

        verify(courseRepository).save(courseCaptor.capture());
        assertEquals("Reactive Programming", courseCaptor.getValue().getName());
        assertNotNull(courseCaptor.getValue().getCreatedAt());
    }

    @Test
    @DisplayName("update copies only the non-null fields onto the stored course")
    void update_shouldMergeNonNullFieldsAndSave() {
        Course incoming = new Course(
                null, "Reactive Programming II", null, false, null,
                LocalDateTime.of(2026, 3, 1, 10, 0));

        when(courseRepository.findById(ID)).thenReturn(Mono.just(course));
        when(courseRepository.save(any(Course.class))).thenReturn(Mono.just(course));

        StepVerifier.create(courseService.update(ID, incoming))
                .expectNext(course)
                .verifyComplete();

        verify(courseRepository).findById(ID);
        verify(courseRepository).save(courseCaptor.capture());

        Course saved = courseCaptor.getValue();
        assertEquals(ID, saved.getId());
        assertEquals("Reactive Programming II", saved.getName());
        assertEquals("RXP", saved.getAcronyms());
        assertEquals(false, saved.getStatus());
        assertEquals(LocalDateTime.of(2026, 1, 15, 9, 30), saved.getCreatedAt());
    }

    @Test
    @DisplayName("update completes empty and never saves when the id does not exist")
    void update_shouldCompleteEmptyWhenMissing() {
        when(courseRepository.findById(ID)).thenReturn(Mono.empty());

        StepVerifier.create(courseService.update(ID, course))
                .verifyComplete();

        verify(courseRepository).findById(ID);
        verify(courseRepository, times(0)).save(any(Course.class));
    }

    @Test
    @DisplayName("delete emits true when the course exists")
    void delete_shouldReturnTrue() {
        when(courseRepository.findById(ID)).thenReturn(Mono.just(course));
        when(courseRepository.deleteById(ID)).thenReturn(Mono.empty());

        StepVerifier.create(courseService.delete(ID))
                .expectNext(true)
                .verifyComplete();

        verify(courseRepository).findById(ID);
        verify(courseRepository).deleteById(ID);
    }

    @Test
    @DisplayName("delete completes empty when the course does not exist")
    void delete_shouldCompleteEmptyWhenMissing() {
        when(courseRepository.findById(ID)).thenReturn(Mono.empty());

        StepVerifier.create(courseService.delete(ID))
                .verifyComplete();

        verify(courseRepository).findById(ID);
        verify(courseRepository, times(0)).deleteById(ID);
    }
}
