package com.example.auction_service.controller;

import com.example.auction_service.model.Auction;
import com.example.auction_service.service.AuctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

}
