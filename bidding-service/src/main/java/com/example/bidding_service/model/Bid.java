package com.example.bidding_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name="bids", schema="bids_schema")
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long auctionId;
    private Long bidderId;
    private BigDecimal amount;
    private LocalDateTime bidTime;

    public Bid() {

    }

    @PrePersist
    protected void onCreate(){
        bidTime = LocalDateTime.now();
    }

    public Bid(Long id, Long auctionId, Long bidderId, BigDecimal amount, LocalDateTime bidTime) {
        this.id = id;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.bidTime = bidTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public Long getBidderId() {
        return bidderId;
    }

    public void setBidderId(Long bidderId) {
        this.bidderId = bidderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }
}
