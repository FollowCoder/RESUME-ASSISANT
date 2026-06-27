package com.resume.repository;

import com.resume.model.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByUserIdOrderByStartedAtDesc(Long userId);

    List<Interview> findByUserIdAndStatus(Long userId, String status);
}
