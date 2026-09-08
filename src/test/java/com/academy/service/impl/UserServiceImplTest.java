package com.academy.service.impl;

import com.academy.exception.ModelNotFoundException;
import com.academy.model.AssignedRole;
import com.academy.model.Role;
import com.academy.model.User;
import com.academy.repository.IUserRepository;
import com.academy.service.IRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String ID = "68c0a3a3a3a3a3a3a3a3a3a3";
    private static final String ROLE_ID = "68c0b1b1b1b1b1b1b1b1b1b1";
    private static final String RAW_PASSWORD = "s3cret-pass";
    private static final String ENCODED_PASSWORD = "$2a$10$encoded";

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IRoleService roleService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private Role role;
    private User user;

    @BeforeEach
    void setUp() {
        role = new Role(ROLE_ID, "ADMIN", true, LocalDateTime.of(2026, 1, 15, 9, 30), null);

        user = new User(
                ID,
                "ana.torres",
                RAW_PASSWORD,
                true,
                List.of(AssignedRole.builder().id(ROLE_ID).build()),
                LocalDateTime.of(2026, 1, 20, 10, 0),
                null);
    }

    @Test
    @DisplayName("findAll emits every user returned by the repository")
    void findAll_shouldEmitAllUsers() {
        User other = new User(
                "68c0a3a3a3a3a3a3a3a3a3a4", "luis.perez", ENCODED_PASSWORD, true,
                List.of(AssignedRole.builder().id(ROLE_ID).build()),
                LocalDateTime.of(2026, 2, 1, 8, 0), null);

        when(userRepository.findAll()).thenReturn(Flux.just(user, other));

        StepVerifier.create(userService.findAll())
                .expectNext(user)
                .expectNext(other)
                .verifyComplete();

        verify(userRepository).findAll();
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("findAll completes empty when there are no users")
    void findAll_shouldCompleteEmpty() {
        when(userRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(userService.findAll())
                .verifyComplete();

        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("findById emits the matching user")
    void findById_shouldReturnUser() {
        when(userRepository.findById(ID)).thenReturn(Mono.just(user));

        StepVerifier.create(userService.findById(ID))
                .expectNext(user)
                .verifyComplete();

        verify(userRepository).findById(ID);
    }

    @Test
    @DisplayName("findById completes empty when the id does not exist")
    void findById_shouldCompleteEmptyWhenMissing() {
        when(userRepository.findById(ID)).thenReturn(Mono.empty());

        StepVerifier.create(userService.findById(ID))
                .verifyComplete();

        verify(userRepository).findById(ID);
    }

    @Test
    @DisplayName("save embeds the resolved roles and never persists the raw password")
    void save_shouldPersistUser() {
        when(roleService.findAllById(anyIterable())).thenReturn(Flux.just(role));
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

        StepVerifier.create(userService.save(user))
                .expectNext(user)
                .verifyComplete();

        verify(userRepository).save(userCaptor.capture());

        User saved = userCaptor.getValue();
        assertEquals("ana.torres", saved.getUsername());
        assertEquals(ENCODED_PASSWORD, saved.getPassword());
        assertNotEquals(RAW_PASSWORD, saved.getPassword());
        assertEquals(1, saved.getRoles().size());
        assertEquals("ADMIN", saved.getRoles().getFirst().getName());
    }

    @Test
    @DisplayName("save fails and never persists when a requested role does not exist")
    void save_shouldFailWhenRoleMissing() {
        User incoming = new User(
                ID, "ana.torres", RAW_PASSWORD, true,
                List.of(
                        AssignedRole.builder().id(ROLE_ID).build(),
                        AssignedRole.builder().id("68c0b1b1b1b1b1b1b1b1b1b2").build()),
                null, null);

        when(roleService.findAllById(anyIterable())).thenReturn(Flux.just(role));

        StepVerifier.create(userService.save(incoming))
                .verifyError(ModelNotFoundException.class);

        verify(userRepository, times(0)).save(any(User.class));
    }

    @Test
    @DisplayName("update copies only the non-null fields and re-encodes a supplied password")
    void update_shouldMergeNonNullFieldsAndSave() {
        User incoming = new User(
                null, "ana.torres.new", RAW_PASSWORD, null, null, null, null);

        when(userRepository.findById(ID)).thenReturn(Mono.just(user));
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

        StepVerifier.create(userService.update(ID, incoming))
                .expectNext(user)
                .verifyComplete();

        verify(userRepository).findById(ID);
        verify(userRepository).save(userCaptor.capture());

        User saved = userCaptor.getValue();
        assertEquals(ID, saved.getId());
        assertEquals("ana.torres.new", saved.getUsername());
        assertEquals(ENCODED_PASSWORD, saved.getPassword());
        assertEquals(true, saved.getStatus());
        assertEquals(1, saved.getRoles().size());
    }

    @Test
    @DisplayName("update resolves the roles when the payload carries them")
    void update_shouldResolveRoles() {
        Role added = new Role("68c0b1b1b1b1b1b1b1b1b1b2", "STUDENT", true, null, null);
        User incoming = new User(
                null, null, null, null,
                List.of(
                        AssignedRole.builder().id(ROLE_ID).build(),
                        AssignedRole.builder().id(added.getId()).build()),
                null, null);

        when(userRepository.findById(ID)).thenReturn(Mono.just(user));
        when(roleService.findAllById(anyIterable())).thenReturn(Flux.just(role, added));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

        StepVerifier.create(userService.update(ID, incoming))
                .expectNext(user)
                .verifyComplete();

        verify(userRepository).save(userCaptor.capture());

        User saved = userCaptor.getValue();
        assertEquals(2, saved.getRoles().size());
        assertEquals("STUDENT", saved.getRoles().getLast().getName());
        assertEquals(RAW_PASSWORD, saved.getPassword());
        verify(passwordEncoder, times(0)).encode(any());
    }

    @Test
    @DisplayName("update completes empty and never saves when the id does not exist")
    void update_shouldCompleteEmptyWhenMissing() {
        when(userRepository.findById(ID)).thenReturn(Mono.empty());

        StepVerifier.create(userService.update(ID, user))
                .verifyComplete();

        verify(userRepository).findById(ID);
        verify(userRepository, times(0)).save(any(User.class));
    }

    @Test
    @DisplayName("delete emits true when the user exists")
    void delete_shouldReturnTrue() {
        when(userRepository.findById(ID)).thenReturn(Mono.just(user));
        when(userRepository.deleteById(ID)).thenReturn(Mono.empty());

        StepVerifier.create(userService.delete(ID))
                .expectNext(true)
                .verifyComplete();

        verify(userRepository).findById(ID);
        verify(userRepository).deleteById(ID);
    }

    @Test
    @DisplayName("delete completes empty when the user does not exist")
    void delete_shouldCompleteEmptyWhenMissing() {
        when(userRepository.findById(ID)).thenReturn(Mono.empty());

        StepVerifier.create(userService.delete(ID))
                .verifyComplete();

        verify(userRepository).findById(ID);
        verify(userRepository, times(0)).deleteById(ID);
    }
}
