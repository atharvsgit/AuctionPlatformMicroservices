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

}
