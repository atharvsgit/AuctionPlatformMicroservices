package com.example.notification_service.consumer;

import com.example.notification_service.dto.AuctionEndedEvent;
import com.example.notification_service.dto.BidPlacedEvent;
import com.example.notification_service.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationConsumer {

    @Autowired
    private EmailService emailService;

    @KafkaListener(topics="bid-events")
    public void consumeBidEvent(BidPlacedEvent e){
        System.out.println("NEW NOTIF - ");
        System.out.println("Alerting Seller ID - "+e.getSellerId());
        try{
            String se = "atharvcodes@gmail.com";
            emailService.sendBidNotification(se, e.getAuctionId(), e.getAmount());
        } catch(Exception ex){
            System.err.println("Failed to send email!!! "+ex.getMessage());
        }
    }

    @KafkaListener(topics="auction-events")
    public void consumeAuctionEndedEvent(AuctionEndedEvent e){
        System.out.println("Auction ended event received!");
        System.out.println("Closing auction ID - "+e.getAuctionId());
        try{
            String email = "atharvcodes@gmail.com";
            emailService.sendAuctionEndedEmail(email, email, e.getAuctionId());
        } catch(Exception ex){
            System.err.println("Failed to send end of auction mails. Reason - "+ex.getMessage());
        }
    }

}
