package com.example.auction_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


public class AuctionEndedEvent {
    private Long auctionId;
    private Long sellerId;

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public AuctionEndedEvent() {
    }

    public AuctionEndedEvent(Long auctionId, Long sellerId) {
        this.auctionId = auctionId;
        this.sellerId = sellerId;
    }
}
