package com.hycora.backend.domain.leader.service;

import com.hycora.backend.domain.leader.dto.LeaderImageDto;
import com.hycora.backend.global.image.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class LeaderImageService {

    private final ImageStorageService imageStorageService;

    public LeaderImageDto.Response upload(String name, MultipartFile image) {
        ImageStorageService.StoredImage storedImage = imageStorageService.storeLeaderPhoto(name, image);
        return new LeaderImageDto.Response(storedImage.imageUrl());
    }

    public LeaderImageDto.Response get(String name) {
        return new LeaderImageDto.Response(imageStorageService.getLeaderPhotoUrl(name));
    }

    public void delete(String name) {
        imageStorageService.deleteLeaderPhoto(name);
    }
}
