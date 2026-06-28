package com.hycora.backend.domain.activity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hycora.backend.domain.activity.entity.Activity;
import com.hycora.backend.domain.activity.repository.ActivityRepository;
import com.hycora.backend.global.image.ImageStorageService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActivityServiceTest {

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
