package ru.aston.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.aston.userservice.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}