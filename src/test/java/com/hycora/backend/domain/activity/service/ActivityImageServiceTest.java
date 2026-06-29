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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
        when(activityRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(activity));
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
    void uploadCleansFilesWhenTransactionRollsBackAfterFlush() throws Exception {
        TransactionSynchronizationManager.initSynchronization();
        try {
            ActivityImageDto.Response response = activityImageService.upload(
                    3L,
                    List.of(imageFile("activity.jpg", "image/jpeg"))
            );

            Path storedFile = uploadRoot.resolve("activities")
                    .resolve("3")
                    .resolve(response.getImages().getFirst().getImageId() + ".jpg");
            assertThat(storedFile).isRegularFile();

            List<TransactionSynchronization> synchronizations =
                    TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations).hasSize(1);
            synchronizations.getFirst().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

            assertThat(uploadRoot.resolve("activities").resolve("3")).doesNotExist();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void rejectsUploadWhenExistingAndNewImagesExceedTen() throws Exception {
        activity.updateImages(objectMapper.writeValueAsString(List.of(
                "/existing-1.jpg", "/existing-2.jpg", "/existing-3.jpg",
                "/existing-4.jpg", "/existing-5.jpg", "/existing-6.jpg",
                "/existing-7.jpg", "/existing-8.jpg", "/existing-9.jpg"
        )));

        assertThatThrownBy(() -> activityImageService.upload(
                3L,
                List.of(
                        imageFile("first.jpg", "image/jpeg"),
                        imageFile("second.jpg", "image/jpeg")
                )
        )).isInstanceOf(ImageUploadException.class)
                .hasMessage("활동 이미지는 총 10개까지 업로드할 수 있습니다.")
                .extracting(exception -> ((ImageUploadException) exception).getStatus().value())
                .isEqualTo(400);

        assertThat(uploadRoot.resolve("activities").resolve("3")).doesNotExist();
        verify(activityRepository, never()).saveAndFlush(any(Activity.class));
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
        when(activityRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

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

    @Test
    void deleteRestoresMetadataWhenFileDeletionFails() {
        ActivityRepository repository = mock(ActivityRepository.class);
        ImageStorageService storageService = mock(ImageStorageService.class);
        UUID imageId = UUID.randomUUID();
        String imageUrl = ImageStorageService.activityImageUrl(3L, imageId);
        String originalImages = "[\"" + imageUrl + "\"]";
        Activity existing = Activity.builder().id(3L).images(originalImages).build();
        when(repository.findByIdForUpdate(3L)).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(any(Activity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storageService.activityImageExists(3L, imageId)).thenReturn(true);
        doThrow(ImageUploadException.storage("이미지 파일을 삭제할 수 없습니다.", new RuntimeException()))
                .when(storageService).deleteActivityImage(3L, imageId);
        ActivityImageService service = new ActivityImageService(repository, storageService, objectMapper);

        assertThatThrownBy(() -> service.delete(3L, imageId))
                .isInstanceOf(ImageUploadException.class)
                .hasMessage("이미지 파일을 삭제할 수 없습니다.");

        assertThat(existing.getImages()).isEqualTo(originalImages);
        verify(repository).saveAndFlush(existing);
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
