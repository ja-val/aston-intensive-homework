package ru.aston.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserUpdateRequest {
    @NotBlank
    @Size(max = 100)
    String name;

    @NotBlank
    @Email
    @Size(max = 150)
    String email;

    @Min(0)
    @Max(150)
    int age;
}