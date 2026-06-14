package ru.aston.userservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import ru.aston.userservice.dto.UserRequest;
import ru.aston.userservice.dto.UserResponse;
import ru.aston.userservice.dto.UserUpdateRequest;
import ru.aston.userservice.entity.User;
import ru.aston.userservice.event.UserEvent;
import ru.aston.userservice.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KafkaTemplate<String, UserEvent> kafkaTemplate;   // <-- новый мок

    @InjectMocks
    private UserService userService;


    @Test
    void createUser_ValidData_ReturnsUserResponse() {
        UserRequest request = UserRequest.builder()
                .name("  Kirill  ")
                .email("KIRILL@TEST.com")
                .age(30)
                .build();

        User savedUser = User.builder()
                .id(1L)
                .name("Kirill")
                .email("kirill@test.com")
                .age(30)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse result = userService.createUser(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Kirill");
        assertThat(result.getEmail()).isEqualTo("kirill@test.com");
        assertThat(result.getAge()).isEqualTo(30);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User capturedUser = captor.getValue();
        assertThat(capturedUser.getName()).isEqualTo("Kirill");
        assertThat(capturedUser.getEmail()).isEqualTo("kirill@test.com");
        assertThat(capturedUser.getAge()).isEqualTo(30);

        verify(kafkaTemplate).send(eq("user-events"), any(UserEvent.class));
        ArgumentCaptor<UserEvent> eventCaptor = ArgumentCaptor.forClass(UserEvent.class);
        verify(kafkaTemplate).send(eq("user-events"), eventCaptor.capture());
        UserEvent event = eventCaptor.getValue();
        assertThat(event.getOperation()).isEqualTo("CREATE");
        assertThat(event.getEmail()).isEqualTo("kirill@test.com");
    }

    @Test
    void createUser_InvalidName_ThrowsException() {
        UserRequest request = UserRequest.builder()
                .name("")
                .email("valid@ex.com")
                .age(20)
                .build();

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Name cannot be blank");
        verifyNoInteractions(userRepository);
        verifyNoInteractions(kafkaTemplate);  // Kafka не должен вызываться
    }

    @Test
    void createUser_InvalidEmail_ThrowsException() {
        UserRequest request = UserRequest.builder()
                .name("Masha")
                .email("wrong")
                .age(25)
                .build();

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email");
        verifyNoInteractions(userRepository);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void findUserById_Existing_ReturnsUserResponse() {
        User mockUser = User.builder()
                .id(5L)
                .name("Sergey")
                .email("s@example.com")
                .age(25)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(5L)).thenReturn(Optional.of(mockUser));

        Optional<UserResponse> found = userService.findUserById(5L);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(5L);
        assertThat(found.get().getName()).isEqualTo("Sergey");
    }

    @Test
    void findUserById_NotFound_ReturnsEmpty() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<UserResponse> found = userService.findUserById(99L);
        assertThat(found).isEmpty();
    }

    @Test
    void findUserById_NullId_ThrowsException() {
        assertThatThrownBy(() -> userService.findUserById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid id");
    }

    @Test
    void updateUser_ValidData_ReturnsUpdatedUserResponse() {
        Long userId = 2L;
        User existing = User.builder()
                .id(userId)
                .name("Old")
                .email("old@mail.com")
                .age(20)
                .createdAt(LocalDateTime.now())
                .build();

        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .name("New Name")
                .email("NEW@MAIL.com")
                .age(30)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse result = userService.updateUser(userId, updateRequest);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getEmail()).isEqualTo("new@mail.com");
        assertThat(result.getAge()).isEqualTo(30);

        verify(userRepository).save(existing);
        assertThat(existing.getName()).isEqualTo("New Name");
        assertThat(existing.getEmail()).isEqualTo("new@mail.com");
        assertThat(existing.getAge()).isEqualTo(30);

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void updateUser_UserNotFound_ThrowsException() {
        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .name("Name")
                .email("any@mail.com")
                .age(30)
                .build();

        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.updateUser(99L, updateRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
        verify(userRepository, never()).save(any());
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void deleteUser_Valid_CallsRepositoryAndSendsEvent() {
        Long userId = 10L;
        User userToDelete = User.builder()
                .id(userId)
                .name("Test")
                .email("test@example.com")
                .age(20)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(userToDelete));

        userService.deleteUser(userId);

        verify(userRepository).delete(userToDelete);

        ArgumentCaptor<UserEvent> eventCaptor = ArgumentCaptor.forClass(UserEvent.class);
        verify(kafkaTemplate).send(eq("user-events"), eventCaptor.capture());
        UserEvent event = eventCaptor.getValue();
        assertThat(event.getOperation()).isEqualTo("DELETE");
        assertThat(event.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void deleteUser_UserNotFound_ThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
        verify(userRepository, never()).delete(any());
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void getAllUsers_ReturnsListOfUserResponse() {
        User user1 = User.builder()
                .id(1L)
                .name("User1")
                .email("u1@ex.com")
                .age(25)
                .createdAt(LocalDateTime.now())
                .build();
        User user2 = User.builder()
                .id(2L)
                .name("User2")
                .email("u2@ex.com")
                .age(30)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        List<UserResponse> result = userService.getAllUsers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
    }
}