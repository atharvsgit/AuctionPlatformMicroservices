package com.example.auction_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="auctions", schema="auctions_schema")
public class Auction implements Serializable {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    private String title;
    private String description;
    private BigDecimal startingPrice;
    private BigDecimal currentBid;
    private String status;
    private LocalDateTime endTime;

    private Long sellerId;


}
