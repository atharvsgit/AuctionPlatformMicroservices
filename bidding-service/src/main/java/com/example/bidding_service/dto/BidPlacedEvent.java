package com.example.bidding_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BidPlacedEvent {
    private Long auctionId;
    private Long bidderId;
    private Long sellerId;
    private BigDecimal amount;
}
