package com.hycora.backend.domain.pastevent.repository;

import com.hycora.backend.domain.pastevent.entity.PastEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PastEventRepository extends JpaRepository<PastEvent, Long> {
    List<PastEvent> findAllByOrderByOrderAsc();
}
