package com.example.auction_service.controller;

import com.example.auction_service.model.Auction;
import com.example.auction_service.service.AuctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/auctions")
public class AuctionController {

    @Autowired
    private AuctionService auctionService;

    @PostMapping
    public ResponseEntity<Auction> createAuction(@RequestBody Auction auction, @RequestHeader("X-User-Id") Long userId){
        auction.setSellerId(userId);
        return new ResponseEntity<>(auctionService.createAuction(auction), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Auction>> getAllAuctions(){
        return ResponseEntity.ok(auctionService.getAllAuctions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Auction> getAuctionById(@PathVariable Long id){
        return ResponseEntity.ok(auctionService.getAuctionById(id));
    }

    @PutMapping("/{id}/bid")
    public ResponseEntity<Void> updateAuctionPrice(@PathVariable("id") Long id,
                                                   @RequestParam("newPrice") BigDecimal newPrice){
        auctionService.updateAuctionPrice(id, newPrice);
        return ResponseEntity.ok().build();
    }

}
