package com.hycora.backend.domain.leader.controller;

import com.hycora.backend.domain.leader.service.LeaderImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/leaders")
@RequiredArgsConstructor
public class LeaderImageController {

    private final LeaderImageService leaderImageService;

    @PostMapping(value = "/{name}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @PathVariable String name,
            @RequestPart("photo") MultipartFile photo
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaderImageService.upload(name, photo));
    }

    @DeleteMapping("/{name}/photo")
    public ResponseEntity<?> delete(@PathVariable String name) {
        leaderImageService.delete(name);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
