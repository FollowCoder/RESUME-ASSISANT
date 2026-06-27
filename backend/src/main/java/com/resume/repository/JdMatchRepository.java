package com.resume.repository;

import com.resume.model.entity.JdMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JdMatchRepository extends JpaRepository<JdMatch, Long> {

    List<JdMatch> findByUserIdOrderByCreatedAtDesc(Long userId);
}
