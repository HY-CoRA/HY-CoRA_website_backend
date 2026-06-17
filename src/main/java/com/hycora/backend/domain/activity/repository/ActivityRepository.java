package com.hycora.backend.domain.activity.repository;

import com.hycora.backend.domain.activity.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findAllByStatus(String status);

    @Query("SELECT a FROM Activity a WHERE a.status = 'recruiting' AND a.recruitEnd IS NOT NULL AND a.recruitEnd < :today")
    List<Activity> findRecruitingExpired(@Param("today") LocalDate today);

    List<Activity> findAllByStatusIn(List<String> statuses);
}
