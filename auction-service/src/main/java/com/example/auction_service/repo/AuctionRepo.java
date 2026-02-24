package com.example.auction_service.repo;

import com.example.auction_service.model.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuctionRepo extends JpaRepository<Auction, Long> {
    List<Auction> findByEndTimeBeforeAndStatus(LocalDateTime time, String status);
}
