package com.hycora.backend.domain.pastevent.service;

import com.hycora.backend.domain.pastevent.dto.PastEventDto;
import com.hycora.backend.domain.pastevent.entity.PastEvent;
import com.hycora.backend.domain.pastevent.repository.PastEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PastEventService {

    private final PastEventRepository pastEventRepository;

    public List<PastEventDto.Response> getAll() {
        return pastEventRepository.findAllByOrderByOrderAsc().stream()
                .map(PastEventDto.Response::from)
                .toList();
    }

    @Transactional
    public PastEventDto.Response create(PastEventDto.Request req) {
        PastEvent event = PastEvent.builder()
                .imageUrl(req.getImageUrl())
                .title(req.getTitle())
                .description(req.getDescription())
                .order(req.getOrder())
                .build();
        return PastEventDto.Response.from(pastEventRepository.save(event));
    }

    @Transactional
    public PastEventDto.Response update(Long id, PastEventDto.Request req) {
        PastEvent event = pastEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Past event not found"));
        event.update(req.getImageUrl(), req.getTitle(), req.getDescription(), req.getOrder());
        return PastEventDto.Response.from(event);
    }

    @Transactional
    public void delete(Long id) {
        PastEvent event = pastEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Past event not found"));
        pastEventRepository.delete(event);
    }

    @Transactional
    public void reorder(List<String> ids) {
        for (int i = 0; i < ids.size(); i++) {
            Long id = Long.parseLong(ids.get(i));
            PastEvent event = pastEventRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Past event not found"));
            event.updateOrder(i);
        }
    }
}
