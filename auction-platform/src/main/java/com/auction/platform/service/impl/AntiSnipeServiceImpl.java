package com.auction.platform.service.impl;

import com.auction.platform.entity.Auction;
import com.auction.platform.service.AntiSnipeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Slf4j
public class AntiSnipeServiceImpl implements AntiSnipeService {

    @Value("${app.auction.anti-snipe.window-seconds}")
    private long windowSeconds;

    @Value("${app.auction.anti-snipe.extension-minutes}")
    private long extensionMinutes;

    @Override
    public void applyIfWithinWindow(Auction auction) {
        LocalDateTime now = LocalDateTime.now();
        long secondsRemaining = Duration.between(now, auction.getEndDate()).getSeconds();

        if (secondsRemaining >= 0 && secondsRemaining <= windowSeconds) {
            LocalDateTime extendedEndDate = auction.getEndDate().plusMinutes(extensionMinutes);
            auction.setEndDate(extendedEndDate);
            auction.setEndingSoonNotified(false); // allow a fresh "ending soon" reminder once the new deadline approaches
            log.info("Anti-sniping triggered for auction {} — extended endDate to {}",
                    auction.getId(), extendedEndDate);
        }
    }
}
