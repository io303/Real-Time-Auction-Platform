package com.auction.platform.service.search;

import com.auction.platform.entity.Auction;
import com.auction.platform.entity.enums.AuctionStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

public final class AuctionSpecifications {

    private AuctionSpecifications() {
    }

    public static Specification<Auction> hasKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null; // Specification.where(null) is safely ignored by Spring Data
        }
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)
        );
    }

    public static Specification<Auction> hasCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Auction> hasSeller(Long sellerId) {
        if (sellerId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("seller").get("id"), sellerId);
    }

    public static Specification<Auction> priceAtLeast(BigDecimal minPrice) {
        if (minPrice == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(effectivePrice(root, cb), minPrice);
    }

    public static Specification<Auction> priceAtMost(BigDecimal maxPrice) {
        if (maxPrice == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(effectivePrice(root, cb), maxPrice);
    }

    public static Specification<Auction> hasStatusIn(List<AuctionStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    /** COALESCE(current_highest_bid, starting_price) — what a buyer actually means by "price". */
    private static Expression<BigDecimal> effectivePrice(Root<Auction> root, CriteriaBuilder cb) {
        return cb.coalesce(root.get("currentHighestBid"), root.get("startingPrice"));
    }
}
