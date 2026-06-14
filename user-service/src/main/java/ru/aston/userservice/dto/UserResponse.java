package ru.aston.userservice.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class UserResponse {
    Long id;
    String name;
    String email;
    int age;
    LocalDateTime createdAt;
}