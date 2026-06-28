package com.hycora.backend.global.image;

import com.hycora.backend.domain.activity.controller.ActivityImageController;
import com.hycora.backend.domain.activity.dto.ActivityImageDto;
import com.hycora.backend.domain.activity.service.ActivityImageService;
import com.hycora.backend.domain.leader.controller.LeaderImageController;
import com.hycora.backend.domain.leader.dto.LeaderImageDto;
import com.hycora.backend.domain.leader.service.LeaderImageService;
import com.hycora.backend.global.auth.JwtAuthenticationFilter;
import com.hycora.backend.global.auth.JwtProvider;
import com.hycora.backend.global.auth.RateLimitFilter;
import com.hycora.backend.global.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {LeaderImageController.class, ActivityImageController.class})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RateLimitFilter.class,
        ImageExceptionHandler.class
})
class ImageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    LeaderImageService leaderImageService;

    @MockitoBean
    ActivityImageService activityImageService;

    @MockitoBean
    JwtProvider jwtProvider;

    @Test
    @WithMockUser(roles = "ADMIN")
    void authenticatedAdminCanUploadLeaderPhoto() throws Exception {
        MockMultipartFile image =
                new MockMultipartFile("image", "leader.jpg", "image/jpeg", new byte[]{1});
        when(leaderImageService.upload(eq("최관우"), any()))
                .thenReturn(new LeaderImageDto.Response("/uploads/leaders/%EC%B5%9C%EA%B4%80%EC%9A%B0.jpg"));

        mockMvc.perform(multipart("/api/leaders/{name}/photo", "최관우").file(image))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl")
                        .value("/uploads/leaders/%EC%B5%9C%EA%B4%80%EC%9A%B0.jpg"));
    }

    @Test
    void unauthenticatedLeaderUploadIsRejected() throws Exception {
        MockMultipartFile image =
                new MockMultipartFile("image", "leader.jpg", "image/jpeg", new byte[]{1});

        mockMvc.perform(multipart("/api/leaders/{name}/photo", "최관우").file(image))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void leaderPhotoGetIsPublicAndMissingPhotoReturns404() throws Exception {
        when(leaderImageService.get("최관우"))
                .thenReturn(new LeaderImageDto.Response("/uploads/leaders/photo.jpg"));
        when(leaderImageService.get("없는사람"))
                .thenThrow(ImageUploadException.notFound("임원진 사진을 찾을 수 없습니다."));

        mockMvc.perform(get("/api/leaders/{name}/photo", "최관우"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value("/uploads/leaders/photo.jpg"));
        mockMvc.perform(get("/api/leaders/{name}/photo", "없는사람"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("임원진 사진을 찾을 수 없습니다."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void authenticatedAdminCanDeleteLeaderPhoto() throws Exception {
        mockMvc.perform(delete("/api/leaders/{name}/photo", "최관우"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(leaderImageService).delete("최관우");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void authenticatedAdminCanUploadMultipleActivityImages() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(activityImageService.upload(eq(3L), anyList())).thenReturn(new ActivityImageDto.Response(List.of(
                new ActivityImageDto.Image(first.toString(), "/uploads/activities/3/" + first + ".jpg"),
                new ActivityImageDto.Image(second.toString(), "/uploads/activities/3/" + second + ".jpg")
        )));

        mockMvc.perform(multipart("/api/activities/{id}/images", 3L)
                        .file(new MockMultipartFile("images", "one.jpg", "image/jpeg", new byte[]{1}))
                        .file(new MockMultipartFile("images", "two.png", "image/png", new byte[]{2})))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.images.length()").value(2))
                .andExpect(jsonPath("$.images[0].imageId").value(first.toString()));
    }

    @Test
    void unauthenticatedActivityUploadAndDeleteAreRejected() throws Exception {
        mockMvc.perform(multipart("/api/activities/{id}/images", 3L)
                        .file(new MockMultipartFile("images", "one.jpg", "image/jpeg", new byte[]{1})))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/activities/{id}/images/{imageId}", 3L, UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsInvalidUuidAndMapsValidationAndSizeErrors() throws Exception {
        mockMvc.perform(delete("/api/activities/{id}/images/{imageId}", 3L, "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("imageId는 올바른 UUID여야 합니다."));

        doThrow(ImageUploadException.invalid("허용되지 않은 이미지입니다."))
                .when(leaderImageService).delete("잘못된이름");
        mockMvc.perform(delete("/api/leaders/{name}/photo", "잘못된이름"))
                .andExpect(status().isBadRequest());

        when(activityImageService.upload(eq(3L), anyList())).thenThrow(ImageUploadException.tooLarge());
        mockMvc.perform(multipart("/api/activities/{id}/images", 3L)
                        .file(new MockMultipartFile("images", "large.jpg", "image/jpeg", new byte[]{1})))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void authenticatedAdminCanDeleteSpecificActivityImage() throws Exception {
        UUID imageId = UUID.randomUUID();

        mockMvc.perform(delete("/api/activities/{id}/images/{imageId}", 3L, imageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(activityImageService).delete(3L, imageId);
    }
}
