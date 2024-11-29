package com.code.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.code.entities.User;

public interface UserRepository extends JpaRepository<User,Integer> {
    Optional<User> findByUserName(String username);
}