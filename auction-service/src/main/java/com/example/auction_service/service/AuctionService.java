package com.example.auction_service.service;

import com.example.auction_service.model.Auction;
import com.example.auction_service.repo.AuctionRepo;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AuctionService {

    @Autowired
    private AuctionRepo auctionRepo;

    @CacheEvict(value = "auctions", allEntries = true)
    public Auction createAuction(Auction auction) {
        auction.setStatus("ACTIVE");
        auction.setCurrentBid(auction.getStartingPrice());
        return auctionRepo.save(auction);
    }

    @Cacheable(value = "auctions")
    public List<Auction> getAllAuctions() {
        return auctionRepo.findAll();
    }

    public Auction getAuctionById(Long id){
        return auctionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found with ID " + id));
    }

    @CacheEvict(value = "auctions", allEntries = true)
    public void updateAuctionPrice(Long auctionId, BigDecimal newBidAmt){
        try {
            Auction auction = getAuctionById(auctionId);

            if(newBidAmt.compareTo(auction.getCurrentBid()) <= 0){
                throw new RuntimeException("Bid must be higher than current price!");
            }

            auction.setCurrentBid(newBidAmt);
            auctionRepo.save(auction);

        } catch (OptimisticLockingFailureException e) {
            throw new RuntimeException("Someone placed a higher bid just before you! Please refresh and try again!");
        }
    }
}