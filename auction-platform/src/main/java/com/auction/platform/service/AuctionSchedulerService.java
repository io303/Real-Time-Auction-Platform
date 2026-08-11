package com.auction.platform.service;

import com.auction.platform.entity.enums.AuctionStatus;
import com.auction.platform.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AuctionSchedulerService {

    private final AuctionRepository auctionRepository;
    private final AuctionLifecycleTransitionService transitionService;

    @Value("${app.notification.ending-soon-minutes}")
    private long endingSoonMinutes;

    @Scheduled(fixedDelayString = "${app.scheduler.start-check-interval-ms}")
    public void startScheduledAuctions() {
        List<Long> candidateIds = auctionRepository.findIdsByStatusAndStartDateBefore(
                AuctionStatus.SCHEDULED, LocalDateTime.now());
        candidateIds.forEach(transitionService::tryStart);
    }

    @Scheduled(fixedDelayString = "${app.scheduler.end-check-interval-ms}")
    public void endLiveAuctions() {
        List<Long> candidateIds = auctionRepository.findIdsByStatusAndEndDateBefore(
                AuctionStatus.LIVE, LocalDateTime.now());
        candidateIds.forEach(transitionService::tryEnd);
    }

    @Scheduled(fixedDelayString = "${app.scheduler.ending-soon-check-interval-ms}")
    public void checkEndingSoonAuctions() {
        LocalDateTime cutoff = LocalDateTime.now().plusMinutes(endingSoonMinutes);
        List<Long> candidateIds = auctionRepository.findIdsForEndingSoonNotification(AuctionStatus.LIVE, cutoff);
        candidateIds.forEach(transitionService::checkEndingSoon);
    }
}
