package com.code.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.code.entities.Post;

public interface PostRepository extends JpaRepository<Post,Integer> {
}