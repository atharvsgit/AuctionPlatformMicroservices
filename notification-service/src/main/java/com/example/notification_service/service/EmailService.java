package com.example.notification_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromMail;

    public void sendBidNotification(String toEmail, Long auctionId, BigDecimal amt){
        SimpleMailMessage smm = new SimpleMailMessage();
        smm.setFrom(fromMail);
        smm.setTo(toEmail);
        smm.setSubject("New Bid Alert : Auction #"+auctionId);
        smm.setText("Great news!\n\nA new bid of $"+amt+" has been set on your item (Auction #"+auctionId+").\n" +
                "\nLogin to your dashboard to see latest activity.");
        javaMailSender.send(smm);
        System.out.println("Email sent to : "+toEmail);
    }

    public void sendAuctionEndedEmail(String sellerEmail, String winnerEmail, Long auctionId){
        SimpleMailMessage smmSeller = new SimpleMailMessage();
        smmSeller.setFrom(fromMail);
        smmSeller.setTo(sellerEmail);
        smmSeller.setSubject("Your Auction #"+auctionId+" has ended!");
        smmSeller.setText("Congratulations! Your auction has officially closed. Check dashboard for further details.");
        javaMailSender.send(smmSeller);
        SimpleMailMessage smmWinner = new SimpleMailMessage();
        smmWinner.setFrom(fromMail);
        smmWinner.setTo(winnerEmail);
        smmWinner.setSubject("You WON! Auction #"+auctionId);
        smmWinner.setText("Great news! The auction has ended and you are the highest bidder for item #" + auctionId + "!");
        javaMailSender.send(smmWinner);

        System.out.println("End of auction mails sent!");
    }

}
