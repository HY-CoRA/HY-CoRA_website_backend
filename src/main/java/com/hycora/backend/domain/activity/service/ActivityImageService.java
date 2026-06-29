package com.hycora.backend.domain.activity.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hycora.backend.domain.activity.dto.ActivityImageDto;
import com.hycora.backend.domain.activity.entity.Activity;
import com.hycora.backend.domain.activity.repository.ActivityRepository;
import com.hycora.backend.global.image.ImageStorageService;
import com.hycora.backend.global.image.ImageUploadException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityImageService {

    private final ActivityRepository activityRepository;
    private final ImageStorageService imageStorageService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ActivityImageDto.Response upload(Long activityId, List<MultipartFile> images) {
        Activity activity = findActivity(activityId);
        List<String> imageUrls = new ArrayList<>(readImages(activity));
        validateTotalImageCount(imageUrls.size(), images);

        List<ImageStorageService.StoredImage> storedImages =
                imageStorageService.storeActivityImages(activityId, images);
        registerRollbackCleanup(activityId, storedImages);

        try {
            imageUrls.addAll(storedImages.stream().map(ImageStorageService.StoredImage::imageUrl).toList());
            activity.updateImages(writeImages(imageUrls));
            activityRepository.saveAndFlush(activity);
        } catch (RuntimeException exception) {
            imageStorageService.cleanupActivityImages(
                    activityId,
                    storedImages.stream().map(ImageStorageService.StoredImage::id).toList()
            );
            throw exception;
        }

        return new ActivityImageDto.Response(storedImages.stream()
                .map(image -> new ActivityImageDto.Image(image.id(), image.imageUrl()))
                .toList());
    }

    @Transactional
    public void delete(Long activityId, UUID imageId) {
        Activity activity = findActivity(activityId);
        String originalImages = activity.getImages();
        String imageUrl = ImageStorageService.activityImageUrl(activityId, imageId);
        List<String> imageUrls = new ArrayList<>(readImages(activity));

        if (!imageUrls.contains(imageUrl) || !imageStorageService.activityImageExists(activityId, imageId)) {
            throw ImageUploadException.notFound("활동 이미지를 찾을 수 없습니다.");
        }

        imageUrls.remove(imageUrl);
        activity.updateImages(writeImages(imageUrls));
        try {
            activityRepository.saveAndFlush(activity);
            imageStorageService.deleteActivityImage(activityId, imageId);
        } catch (RuntimeException exception) {
            activity.updateImages(originalImages);
            throw exception;
        }
    }

    private Activity findActivity(Long activityId) {
        return activityRepository.findByIdForUpdate(activityId)
                .orElseThrow(() -> ImageUploadException.notFound("활동을 찾을 수 없습니다."));
    }

    private List<String> readImages(Activity activity) {
        if (activity.getImages() == null || activity.getImages().isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(activity.getImages(), new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw ImageUploadException.storage("활동 이미지 메타데이터를 읽을 수 없습니다.", exception);
        }
    }

    private void registerRollbackCleanup(
            Long activityId,
            List<ImageStorageService.StoredImage> storedImages
    ) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        List<String> imageIds = storedImages.stream()
                .map(ImageStorageService.StoredImage::id)
                .toList();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    imageStorageService.cleanupActivityImages(activityId, imageIds);
                }
            }
        });
    }

    private void validateTotalImageCount(int currentCount, List<MultipartFile> images) {
        if (images != null && !images.isEmpty()
                && currentCount > ImageStorageService.MAX_ACTIVITY_IMAGES - images.size()) {
            throw ImageUploadException.invalid("활동 이미지는 총 10개까지 업로드할 수 있습니다.");
        }
    }

    private String writeImages(List<String> images) {
        try {
            return objectMapper.writeValueAsString(images);
        } catch (JsonProcessingException exception) {
            throw ImageUploadException.storage("활동 이미지 메타데이터를 저장할 수 없습니다.", exception);
        }
    }
}
