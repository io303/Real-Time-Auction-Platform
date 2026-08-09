package com.auction.platform.service;

import com.auction.platform.dto.response.AuctionImageResponse;
import com.auction.platform.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AuctionImageService {
    List<AuctionImageResponse> addImage(User owner, Long auctionId, MultipartFile file);
    void deleteImage(User owner, Long auctionId, Long imageId);
    AuctionImageResponse setPrimary(User owner, Long auctionId, Long imageId);
}
