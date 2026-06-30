package com.hycora.backend.domain.activity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hycora.backend.domain.activity.dto.ActivityDto;
import com.hycora.backend.domain.activity.entity.Activity;
import com.hycora.backend.domain.activity.repository.ActivityRepository;
import com.hycora.backend.global.image.ImageStorageService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActivityServiceTest {

    @Test
    void createInitializesImagesEmptyInsteadOfAcceptingArbitraryUrls() throws Exception {
        ActivityRepository activityRepository = mock(ActivityRepository.class);
        ImageStorageService imageStorageService = mock(ImageStorageService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        ActivityDto.Request request = objectMapper.readValue("""
                {
                  "title": "새 활동",
                  "images": ["/external/one.jpg", "/external/two.jpg"]
                }
                """, ActivityDto.Request.class);
        when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ActivityService activityService =
                new ActivityService(activityRepository, objectMapper, imageStorageService);

        activityService.create(request);

        var activityCaptor = org.mockito.ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(activityCaptor.capture());
        assertThat(activityCaptor.getValue().getImages()).isEqualTo("[]");
    }

    @Test
    void updatePreservesImagesManagedByDedicatedImageApi() throws Exception {
        ActivityRepository activityRepository = mock(ActivityRepository.class);
        ImageStorageService imageStorageService = mock(ImageStorageService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        Activity activity = Activity.builder()
                .id(3L)
                .images("[\"/uploads/activities/3/existing.jpg\"]")
                .build();
        ActivityDto.Request request = objectMapper.readValue("""
                {
                  "title": "수정된 활동",
                  "schedule": ["수정된 일정"],
                  "images": []
                }
                """, ActivityDto.Request.class);
        when(activityRepository.findById(3L)).thenReturn(Optional.of(activity));
        ActivityService activityService =
                new ActivityService(activityRepository, objectMapper, imageStorageService);

        activityService.update(3L, request);

        assertThat(activity.getImages()).isEqualTo("[\"/uploads/activities/3/existing.jpg\"]");
    }

    @Test
    void deleteKeepsServer03ActivityImageCleanupHook() {
        ActivityRepository activityRepository = mock(ActivityRepository.class);
        ImageStorageService imageStorageService = mock(ImageStorageService.class);
        Activity activity = Activity.builder().id(3L).build();
        when(activityRepository.findById(3L)).thenReturn(Optional.of(activity));
        ActivityService activityService =
                new ActivityService(activityRepository, new ObjectMapper(), imageStorageService);

        activityService.delete(3L);

        var ordered = inOrder(imageStorageService, activityRepository);
        ordered.verify(imageStorageService).deleteActivityDirectory(3L);
        ordered.verify(activityRepository).delete(activity);
    }
}
