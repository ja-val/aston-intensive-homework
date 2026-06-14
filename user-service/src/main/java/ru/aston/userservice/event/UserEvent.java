package ru.aston.userservice.event;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserEvent {
    String operation;
    String email;
}