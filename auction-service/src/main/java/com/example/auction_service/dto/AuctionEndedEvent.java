package com.example.auction_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuctionEndedEvent {
    private Long auctionId;
    private Long sellerId;

}
