package com.example.bidding_service.client;

import com.example.bidding_service.dto.AuctionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@FeignClient(name="AUCTION-SERVICE")
public interface AuctionClient {
    @GetMapping("/api/auctions/{id}")
    AuctionDto getAuctionById(@PathVariable("id") Long id);

    @PutMapping("/api/auctions/{id}/bid")
    void updateAuctionPrice(@PathVariable("id") Long id, @RequestParam("newPrice")BigDecimal newPrice);
}
