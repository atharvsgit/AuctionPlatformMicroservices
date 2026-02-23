package com.example.bidding_service.controller;

import com.example.bidding_service.model.Bid;
import com.example.bidding_service.service.BidService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bids")
public class BidController {

    @Autowired
    private BidService bidService;

    @PostMapping
    public ResponseEntity<?> placeBid(@RequestBody Bid bidReq, @RequestHeader("X-User-Id") Long userId){
        try{
            Bid savedBid = bidService.placeBid(bidReq.getAuctionId(), userId, bidReq.getAmount());
            return new ResponseEntity<>(savedBid, HttpStatus.CREATED);
        } catch (RuntimeException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/auction/{auctionId}")
    public ResponseEntity<List<Bid>> getBidsForAuction(@PathVariable Long auctionId){
        return ResponseEntity.ok(bidService.getBidsForAuction(auctionId));
    }

}
