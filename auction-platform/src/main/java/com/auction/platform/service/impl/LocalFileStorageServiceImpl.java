package com.auction.platform.service.impl;

import com.auction.platform.exception.ApiException;
import com.auction.platform.service.ProfileImageStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class LocalFileStorageServiceImpl implements ProfileImageStorageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB

    @Value("${app.storage.local.base-path}")
    private String basePath;

    @Override
    public String store(MultipartFile file, Long userId) {
        validate(file);

        try {
            String extension = extractExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + extension;

            Path userDir = Path.of(basePath, "profile-images", String.valueOf(userId));
            Files.createDirectories(userDir);

            Path destination = userDir.resolve(filename);
            file.transferTo(destination);

            String relativeUrl = "/uploads/profile-images/" + userId + "/" + filename;
            log.info("Stored profile image for user {} at {}", userId, destination);
            return relativeUrl;
        } catch (IOException e) {
            log.error("Failed to store profile image for user {}", userId, e);
            throw new ApiException("Failed to upload image. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException("File is empty", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ApiException("File size must not exceed 5MB", HttpStatus.BAD_REQUEST);
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new ApiException("Only JPEG, PNG, and WEBP images are allowed", HttpStatus.BAD_REQUEST);
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}
