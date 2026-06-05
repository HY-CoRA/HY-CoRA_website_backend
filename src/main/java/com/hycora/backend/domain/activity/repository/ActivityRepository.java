package com.hycora.backend.domain.activity.repository;

import com.hycora.backend.domain.activity.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findAllByStatus(String status);
}
