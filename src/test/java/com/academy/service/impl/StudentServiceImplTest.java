package com.academy.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.model.Student;
import com.academy.repository.IStudentRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    private static final String ID = "68c0f0f0f0f0f0f0f0f0f0f0";

    @Mock
    private IStudentRepository studentRepository;

    @InjectMocks
    private StudentServiceImpl studentService;

    @Captor
    private ArgumentCaptor<Student> studentCaptor;

    private Student student;

    @BeforeEach
    void setUp() {
        student = new Student(
                ID,
                "Ana",
                "Torres",
                "45871236",
                24,
                "ana.torres@academy.com",
                true,
                LocalDateTime.of(2026, 1, 15, 9, 30),
                null
        );
    }

    @Test
    @DisplayName("findAll emits every student returned by the repository")
    void findAll_shouldEmitAllStudents() {
        Student other = new Student(
                "68c0f0f0f0f0f0f0f0f0f0f1", "Luis", "Rojas", "10293847", 31,
                "luis.rojas@academy.com", true, LocalDateTime.of(2026, 2, 1, 8, 0), null);

        when(studentRepository.findAll()).thenReturn(Flux.just(student, other));

        StepVerifier.create(studentService.findAll())
                .expectNext(student)
                .expectNext(other)
                .verifyComplete();

        verify(studentRepository).findAll();
        verifyNoMoreInteractions(studentRepository);
    }

    @Test
    @DisplayName("findAll completes empty when there are no students")
    void findAll_shouldCompleteEmpty() {
        when(studentRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(studentService.findAll())
                .verifyComplete();

        verify(studentRepository).findAll();
    }

    @Test
    @DisplayName("findById emits the matching student")
    void findById_shouldReturnStudent() {
        when(studentRepository.findById(ID)).thenReturn(Mono.just(student));

        StepVerifier.create(studentService.findById(ID))
                .expectNext(student)
                .verifyComplete();

        verify(studentRepository).findById(ID);
    }

    @Test
    @DisplayName("findById completes empty when the id does not exist")
    void findById_shouldCompleteEmptyWhenMissing() {
        when(studentRepository.findById(ID)).thenReturn(Mono.empty());

        StepVerifier.create(studentService.findById(ID))
                .verifyComplete();

        verify(studentRepository).findById(ID);
    }

    @Test
    @DisplayName("save delegates to the repository and emits the persisted student")
    void save_shouldPersistStudent() {
        when(studentRepository.save(any(Student.class))).thenReturn(Mono.just(student));

        StepVerifier.create(studentService.save(student))
                .expectNext(student)
                .verifyComplete();

        verify(studentRepository).save(studentCaptor.capture());
        assertEquals("Ana", studentCaptor.getValue().getFirstName());
    }

    @Test
    @DisplayName("update assigns the path id to the incoming document and saves it")
    void update_shouldAssignIdAndSave() {
        Student incoming = new Student(
                null, "Ana Maria", "Torres", "45871236", 25,
                "ana.torres@academy.com", true, null, LocalDateTime.of(2026, 3, 1, 10, 0));

        when(studentRepository.findById(ID)).thenReturn(Mono.just(student));
        when(studentRepository.save(any(Student.class))).thenReturn(Mono.just(incoming));

        StepVerifier.create(studentService.update(ID, incoming))
                .expectNext(incoming)
                .verifyComplete();

        verify(studentRepository).findById(ID);
        verify(studentRepository).save(studentCaptor.capture());
        assertEquals(ID, studentCaptor.getValue().getId());
        assertEquals("Ana Maria", studentCaptor.getValue().getFirstName());
    }

    @Test
    @DisplayName("update completes empty and never saves when the id does not exist")
    void update_shouldCompleteEmptyWhenMissing() {
        when(studentRepository.findById(ID)).thenReturn(Mono.empty());

        StepVerifier.create(studentService.update(ID, student))
                .verifyComplete();

        verify(studentRepository).findById(ID);
        verify(studentRepository, times(0)).save(any(Student.class));
    }

    @Test
    @DisplayName("delete emits true when the student exists")
    void delete_shouldReturnTrue() {
        when(studentRepository.findById(ID)).thenReturn(Mono.just(student));
        when(studentRepository.deleteById(ID)).thenReturn(Mono.empty());

        StepVerifier.create(studentService.delete(ID))
                .expectNext(true)
                .verifyComplete();

        verify(studentRepository).findById(ID);
        verify(studentRepository).deleteById(ID);
    }

    @Test
    @DisplayName("delete completes empty when the student does not exist")
    void delete_shouldCompleteEmptyWhenMissing() {
        when(studentRepository.findById(ID)).thenReturn(Mono.empty());

        StepVerifier.create(studentService.delete(ID))
                .verifyComplete();

        verify(studentRepository).findById(ID);
        verify(studentRepository, times(0)).deleteById(ID);
    }
}
