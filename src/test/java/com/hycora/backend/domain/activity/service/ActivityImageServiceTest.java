package com.hycora.backend.domain.activity.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hycora.backend.domain.activity.dto.ActivityImageDto;
import com.hycora.backend.domain.activity.entity.Activity;
import com.hycora.backend.domain.activity.repository.ActivityRepository;
import com.hycora.backend.global.image.ImageStorageService;
import com.hycora.backend.global.image.ImageUploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityImageServiceTest {

    @TempDir
    Path uploadRoot;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ActivityRepository activityRepository;
    private ActivityImageService activityImageService;
    private Activity activity;

    @BeforeEach
    void setUp() throws Exception {
        Files.createDirectories(uploadRoot.resolve("leaders"));
        Files.createDirectories(uploadRoot.resolve("activities"));
        activityRepository = mock(ActivityRepository.class);
        ImageStorageService imageStorageService = new ImageStorageService(uploadRoot.toString());
        activityImageService = new ActivityImageService(activityRepository, imageStorageService, objectMapper);
        activity = Activity.builder().id(3L).images("[\"/existing.jpg\"]").build();
        when(activityRepository.findById(3L)).thenReturn(Optional.of(activity));
        when(activityRepository.saveAndFlush(any(Activity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void uploadAddsGeneratedUrlsToExistingActivityMetadata() throws Exception {
        ActivityImageDto.Response response = activityImageService.upload(
                3L,
                List.of(imageFile("activity.png", "image/png"))
        );

        assertThat(response.getImages()).hasSize(1);
        ActivityImageDto.Image image = response.getImages().getFirst();
        UUID.fromString(image.getImageId());
        assertThat(image.getImageUrl()).isEqualTo("/uploads/activities/3/" + image.getImageId() + ".jpg");
        assertThat(readUrls(activity.getImages())).containsExactly("/existing.jpg", image.getImageUrl());
        assertThat(uploadRoot.resolve("activities").resolve("3").resolve(image.getImageId() + ".jpg"))
                .isRegularFile();
        verify(activityRepository).saveAndFlush(activity);
    }

    @Test
    void uploadCleansFilesWhenDatabaseUpdateFails() throws Exception {
        when(activityRepository.saveAndFlush(any(Activity.class))).thenThrow(new RuntimeException("db failure"));

        assertThatThrownBy(() -> activityImageService.upload(
                3L,
                List.of(imageFile("activity.jpg", "image/jpeg"))
        )).isInstanceOf(RuntimeException.class).hasMessage("db failure");

        assertThat(uploadRoot.resolve("activities").resolve("3")).doesNotExist();
    }

    @Test
    void deleteRemovesOnlyRequestedImageAndMetadata() throws Exception {
        ActivityImageDto.Image first = activityImageService.upload(
                3L,
                List.of(imageFile("first.jpg", "image/jpeg"))
        ).getImages().getFirst();
        ActivityImageDto.Image second = activityImageService.upload(
                3L,
                List.of(imageFile("second.jpg", "image/jpeg"))
        ).getImages().getFirst();

        activityImageService.delete(3L, UUID.fromString(first.getImageId()));

        assertThat(readUrls(activity.getImages()))
                .contains("/existing.jpg", second.getImageUrl())
                .doesNotContain(first.getImageUrl());
        assertThat(uploadRoot.resolve("activities").resolve("3").resolve(first.getImageId() + ".jpg"))
                .doesNotExist();
        assertThat(uploadRoot.resolve("activities").resolve("3").resolve(second.getImageId() + ".jpg"))
                .isRegularFile();
    }

    @Test
    void rejectsMissingActivityAndImageFromAnotherActivity() throws Exception {
        when(activityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityImageService.upload(
                99L,
                List.of(imageFile("activity.jpg", "image/jpeg"))
        )).isInstanceOf(ImageUploadException.class)
                .extracting(exception -> ((ImageUploadException) exception).getStatus().value())
                .isEqualTo(404);

        assertThatThrownBy(() -> activityImageService.delete(3L, UUID.randomUUID()))
                .isInstanceOf(ImageUploadException.class)
                .extracting(exception -> ((ImageUploadException) exception).getStatus().value())
                .isEqualTo(404);
    }

    private MockMultipartFile imageFile(String filename, String contentType) throws Exception {
        BufferedImage image = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String format = filename.endsWith(".png") ? "png" : "jpg";
        ImageIO.write(image, format, output);
        return new MockMultipartFile("images", filename, contentType, output.toByteArray());
    }

    private List<String> readUrls(String json) throws Exception {
        return objectMapper.readValue(json, new TypeReference<>() {
        });
    }
}
