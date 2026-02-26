package com.example.bidding_service.dto;

import lombok.*;

import java.math.BigDecimal;


public class BidPlacedEvent {
    private Long auctionId;
    private Long bidderId;
    private Long sellerId;
    private BigDecimal amount;

    public BidPlacedEvent(Long auctionId) {
        this.auctionId = auctionId;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public Long getBidderId() {
        return bidderId;
    }

    public void setBidderId(Long bidderId) {
        this.bidderId = bidderId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BidPlacedEvent(Long auctionId, Long bidderId, Long sellerId, BigDecimal amount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.sellerId = sellerId;
        this.amount = amount;
    }
}
