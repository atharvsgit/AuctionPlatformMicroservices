package com.example.bidding_service.service;

import com.example.bidding_service.client.AuctionClient;
import com.example.bidding_service.dto.AuctionDto;
import com.example.bidding_service.dto.BidPlacedEvent;
import com.example.bidding_service.model.Bid;
import com.example.bidding_service.repo.BidRepo;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
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

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

//    @Transactional
    @CircuitBreaker(name="auctionServiceBreaker", fallbackMethod = "auctionServiceFallback")
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

//        String msg = String.format("User %d placed a winning bid of $%s on Auction %d", bidderId, amount, auctionId);
//        kafkaTemplate.send("bid-events", msg);

        BidPlacedEvent e = new BidPlacedEvent(auctionId, bidderId, auctionDto.getSellerId(), amount);
        kafkaTemplate.send("bid-events", e);

        return savedBid;
    }

    public Bid auctionServiceFallback(Long auctionId, Long bidderId, BigDecimal amount, Throwable throwable){
        System.err.println("CIRCUIT BREAKER - Auction Service is down! Reason : "+throwable.getMessage());
        throw new RuntimeException("The Auction system is currently experiencing heavy load or is undergoing " +
                "maintenance. Please try your bid again in a few moments.");
    }

    public List<Bid> getBidsForAuction(Long auctionId){
        return bidRepo.findByAuctionIdOrderByAmountDesc(auctionId);
    }

}
