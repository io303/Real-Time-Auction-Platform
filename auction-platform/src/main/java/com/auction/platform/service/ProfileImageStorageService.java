package com.auction.platform.service;

import org.springframework.web.multipart.MultipartFile;

public interface ProfileImageStorageService {
    /** Stores the file and returns a relative URL path (e.g. "/uploads/profile-images/7/abc.jpg"). */
    String store(MultipartFile file, Long userId);
}
