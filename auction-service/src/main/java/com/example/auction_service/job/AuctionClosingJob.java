package com.example.auction_service.job;

import com.example.auction_service.dto.AuctionEndedEvent;
import com.example.auction_service.model.Auction;
import com.example.auction_service.repo.AuctionRepo;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class AuctionClosingJob {

    @Autowired
    private AuctionRepo auctionRepo;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(fixedRate = 6000)
    @Transactional
    public void closeExpiredAuctions(){
        LocalDateTime now = LocalDateTime.now();
        List<Auction> expiredAuctions = auctionRepo.findByEndTimeBeforeAndStatus(now, "ACTIVE");

        if(!expiredAuctions.isEmpty()){
            System.out.println("Found "+expiredAuctions.size()+" expired auctions. Closing now...");
            for(Auction a : expiredAuctions){
                a.setStatus("CLOSED");
                auctionRepo.save(a);

                AuctionEndedEvent event = new AuctionEndedEvent(a.getId(), a.getSellerId());
                kafkaTemplate.send("auction-events", event);

                System.out.println("Auction #"+a.getId()+" has been officially closed!");

                //send mail to owner andwinner
            }
        }
    }

}
