package com.academy.service.impl;

import com.academy.model.Role;
import com.academy.repository.IRoleRepository;
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
class RoleServiceImplTest {

    private static final String ID = "68c0b1b1b1b1b1b1b1b1b1b1";

    @Mock
    private IRoleRepository roleRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    @Captor
    private ArgumentCaptor<Role> roleCaptor;

    private Role role;

    @BeforeEach
    void setUp() {
        role = new Role(
                ID,
                "ADMIN",
                true,
                LocalDateTime.of(2026, 1, 15, 9, 30),
                null
        );
    }

    @Test
    @DisplayName("findAll emits every role returned by the repository")
    void findAll_shouldEmitAllRoles() {
        Role other = new Role(
                "68c0b1b1b1b1b1b1b1b1b1b2", "STUDENT", true,
                LocalDateTime.of(2026, 2, 1, 8, 0), null);

        when(roleRepository.findAll()).thenReturn(Flux.just(role, other));

        StepVerifier.create(roleService.findAll())
                .expectNext(role)
                .expectNext(other)
                .verifyComplete();

        verify(roleRepository).findAll();
        verifyNoMoreInteractions(roleRepository);
    }

    @Test
    @DisplayName("findAll completes empty when there are no roles")
    void findAll_shouldCompleteEmpty() {
        when(roleRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(roleService.findAll())
                .verifyComplete();

        verify(roleRepository).findAll();
    }

    @Test
    @DisplayName("findById emits the matching role")
    void findById_shouldReturnRole() {
        when(roleRepository.findById(ID)).thenReturn(Mono.just(role));

        StepVerifier.create(roleService.findById(ID))
                .expectNext(role)
                .verifyComplete();

        verify(roleRepository).findById(ID);
    }

    @Test
    @DisplayName("findById completes empty when the id does not exist")
    void findById_shouldCompleteEmptyWhenMissing() {
        when(roleRepository.findById(ID)).thenReturn(Mono.empty());

        StepVerifier.create(roleService.findById(ID))
                .verifyComplete();

        verify(roleRepository).findById(ID);
    }

    @Test
    @DisplayName("save delegates to the repository and emits the persisted role")
    void save_shouldPersistRole() {
        when(roleRepository.save(any(Role.class))).thenReturn(Mono.just(role));

        StepVerifier.create(roleService.save(role))
                .expectNext(role)
                .verifyComplete();

        verify(roleRepository).save(roleCaptor.capture());
        assertEquals("ADMIN", roleCaptor.getValue().getName());
        assertNotNull(roleCaptor.getValue().getCreatedAt());
    }

    @Test
    @DisplayName("update copies only the non-null fields onto the stored role")
    void update_shouldMergeNonNullFieldsAndSave() {
        Role incoming = new Role(
                null, "SUPER_ADMIN", null, null,
                LocalDateTime.of(2026, 3, 1, 10, 0));

        when(roleRepository.findById(ID)).thenReturn(Mono.just(role));
        when(roleRepository.save(any(Role.class))).thenReturn(Mono.just(role));

        StepVerifier.create(roleService.update(ID, incoming))
                .expectNext(role)
                .verifyComplete();

        verify(roleRepository).findById(ID);
        verify(roleRepository).save(roleCaptor.capture());

        Role saved = roleCaptor.getValue();
        assertEquals(ID, saved.getId());
        assertEquals("SUPER_ADMIN", saved.getName());
        assertEquals(true, saved.getStatus());
        assertEquals(LocalDateTime.of(2026, 1, 15, 9, 30), saved.getCreatedAt());
    }

    @Test
    @DisplayName("update completes empty and never saves when the id does not exist")
    void update_shouldCompleteEmptyWhenMissing() {
        when(roleRepository.findById(ID)).thenReturn(Mono.empty());

        StepVerifier.create(roleService.update(ID, role))
                .verifyComplete();

        verify(roleRepository).findById(ID);
        verify(roleRepository, times(0)).save(any(Role.class));
    }

    @Test
    @DisplayName("delete emits true when the role exists")
    void delete_shouldReturnTrue() {
        when(roleRepository.findById(ID)).thenReturn(Mono.just(role));
        when(roleRepository.deleteById(ID)).thenReturn(Mono.empty());

        StepVerifier.create(roleService.delete(ID))
                .expectNext(true)
                .verifyComplete();

        verify(roleRepository).findById(ID);
        verify(roleRepository).deleteById(ID);
    }

    @Test
    @DisplayName("delete completes empty when the role does not exist")
    void delete_shouldCompleteEmptyWhenMissing() {
        when(roleRepository.findById(ID)).thenReturn(Mono.empty());

        StepVerifier.create(roleService.delete(ID))
                .verifyComplete();

        verify(roleRepository).findById(ID);
        verify(roleRepository, times(0)).deleteById(ID);
    }
}
