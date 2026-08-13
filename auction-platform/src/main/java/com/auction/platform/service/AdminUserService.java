package com.auction.platform.service;

import com.auction.platform.dto.response.AdminUserResponse;
import com.auction.platform.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {
    Page<AdminUserResponse> listUsers(String keyword, Pageable pageable);
    void banUser(User admin, Long userId);
    void unbanUser(User admin, Long userId);
}
