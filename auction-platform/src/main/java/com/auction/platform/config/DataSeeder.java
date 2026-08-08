package com.auction.platform.config;

import com.auction.platform.entity.Role;
import com.auction.platform.entity.enums.RoleType;
import com.auction.platform.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(String... args) {
        for (RoleType type : RoleType.values()) {
            roleRepository.findByName(type).orElseGet(() -> {
                log.info("Seeding missing role: {}", type);
                return roleRepository.save(Role.builder().name(type).build());
            });
        }
    }
}
