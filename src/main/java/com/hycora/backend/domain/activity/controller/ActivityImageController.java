package com.hycora.backend.domain.activity.controller;

import com.hycora.backend.domain.activity.service.ActivityImageService;
import com.hycora.backend.global.image.ImageUploadException;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityImageController {

    private final ActivityImageService activityImageService;

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @PathVariable Long id,
            @RequestPart("images") List<MultipartFile> images
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activityImageService.upload(id, images));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<?> delete(@PathVariable Long id, @PathVariable String imageId) {
        UUID parsedImageId;
        try {
            parsedImageId = UUID.fromString(imageId);
        } catch (IllegalArgumentException exception) {
            throw ImageUploadException.invalid("imageId는 올바른 UUID여야 합니다.");
        }

        activityImageService.delete(id, parsedImageId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
