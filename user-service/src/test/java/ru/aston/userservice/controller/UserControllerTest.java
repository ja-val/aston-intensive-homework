package ru.aston.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.aston.userservice.dto.UserRequest;
import ru.aston.userservice.dto.UserResponse;
import ru.aston.userservice.dto.UserUpdateRequest;
import ru.aston.userservice.service.UserService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createUser_ShouldReturnCreatedUser() throws Exception {
        UserRequest request = UserRequest.builder()
                .name("Kirill")
                .email("kirill@example.com")
                .age(30)
                .build();

        UserResponse response = UserResponse.builder()
                .id(1L)
                .name("Kirill")
                .email("kirill@example.com")
                .age(30)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.createUser(any(UserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Kirill"))
                .andExpect(jsonPath("$.email").value("kirill@example.com"));
    }

    @Test
    void getUser_WhenExists_ShouldReturnUser() throws Exception {
        UserResponse response = UserResponse.builder()
                .id(1L)
                .name("Kirill")
                .email("k@k.com")
                .age(20)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.findUserById(1L)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getUser_WhenNotFound_ShouldReturn404() throws Exception {
        when(userService.findUserById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllUsers_ShouldReturnList() throws Exception {
        when(userService.getAllUsers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void deleteUser_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateUser_ShouldReturnUpdatedUser() throws Exception {
        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .name("Kirill Updated")
                .email("updated@example.com")
                .age(31)
                .build();

        UserResponse response = UserResponse.builder()
                .id(1L)
                .name("Kirill Updated")
                .email("updated@example.com")
                .age(31)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.updateUser(eq(1L), any(UserUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kirill Updated"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));
    }
}