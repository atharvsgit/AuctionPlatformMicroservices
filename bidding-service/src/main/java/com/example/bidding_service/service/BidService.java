package com.example.bidding_service.service;

import com.example.bidding_service.client.AuctionClient;
import com.example.bidding_service.dto.AuctionDto;
import com.example.bidding_service.model.Bid;
import com.example.bidding_service.repo.BidRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BidService {

    @Autowired
    private BidRepo bidRepo;

    @Autowired
    private AuctionClient auctionClient;

    public Bid placeBid(Long auctionId, Long bidderId, BigDecimal amount){
        AuctionDto auctionDto = auctionClient.getAuctionById(auctionId);
        if(!"ACTIVE".equals(auctionDto.getStatus())) throw new RuntimeException("This auction is no longer active");
        if(auctionDto.getEndTime().isBefore(LocalDateTime.now())) throw new RuntimeException("Auction has already " +
                "ended");
        if(auctionDto.getSellerId().equals(bidderId)) throw new RuntimeException("Fraud Alert : You cannot bid on " +
                "your own auction!");
        if(amount.compareTo(auctionDto.getCurrentBid())<=0) throw new RuntimeException("Bid must be higher than " +
                "current price of - "+auctionDto.getCurrentBid());

        Bid bid = new Bid();
        bid.setAuctionId(auctionId);
        bid.setBidderId(bidderId);
        bid.setAmount(amount);
        Bid savedBid = bidRepo.save(bid);

        auctionClient.updateAuctionPrice(auctionId, amount);

        return savedBid;
    }

    public List<Bid> getBidsForAuction(Long auctionId){
        return bidRepo.findByAuctionIdOrderByAmountDesc(auctionId);
    }

}
