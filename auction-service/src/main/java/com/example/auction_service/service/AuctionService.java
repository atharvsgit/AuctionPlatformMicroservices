package com.example.auction_service.service;

import com.example.auction_service.model.Auction;
import com.example.auction_service.repo.AuctionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AuctionService {

    @Autowired
    private AuctionRepo auctionRepo;

    public Auction createAuction(Auction auction) {
        auction.setStatus("ACTIVE");
        auction.setCurrentBid(auction.getStartingPrice());
        return (Auction) auctionRepo.save(auction);
    }

    public List<Auction> getAllAuctions() {
        return auctionRepo.findAll();
    }

    public Auction getAuctionById(Long id){
        return auctionRepo.findById(id).orElseThrow(()->new RuntimeException("Auction not found with ID "+id));
    }

    public void updateAuctionPrice(Long id, BigDecimal price){
        Auction auction = getAuctionById(id);
        auction.setCurrentBid(price);
        auctionRepo.save(auction);
    }
}
