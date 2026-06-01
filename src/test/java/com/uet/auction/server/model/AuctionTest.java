package com.uet.auction.server.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class AuctionTest {

    @Test
    public void testDefaultConstructor() {
        Auction auction = new Auction();
        assertNotNull(auction);
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
        assertEquals(0, auction.getExtensionCount());
        assertNotNull(auction.getBidHistory());
        assertTrue(auction.getBidHistory().isEmpty());
    }

    @Test
    public void testParameterizedConstructorWithAllFields() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusDays(1);
        BigDecimal currentPrice = new BigDecimal("15000");

        Auction auction = new Auction(12, now, 5, 2, 8, now, end, currentPrice, AuctionStatus.OPEN, 3);

        assertEquals(12, auction.getId());
        assertEquals(now, auction.getCreatedAt());
        assertEquals(5, auction.getItemId());
        assertEquals(2, auction.getSellerId());
        assertEquals(8, auction.getHighestBidderId());
        assertEquals(now, auction.getStartTime());
        assertEquals(end, auction.getEndTime());
        assertEquals(currentPrice, auction.getCurrentPrice());
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
        assertEquals(3, auction.getExtensionCount());
        assertNotNull(auction.getBidHistory());
    }

    @Test
    public void testParameterizedConstructorWithItemDetails() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusHours(5);
        BigDecimal startingPrice = new BigDecimal("50000");

        Auction auction = new Auction(8, 3, startingPrice, now, end);

        assertEquals(8, auction.getItemId());
        assertEquals(3, auction.getSellerId());
        assertEquals(startingPrice, auction.getStartingPrice());
        assertEquals(startingPrice, auction.getCurrentPrice());
        assertEquals(now, auction.getStartTime());
        assertEquals(end, auction.getEndTime());
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
        assertEquals(0, auction.getTotalBids());
        assertEquals(0, auction.getExtensionCount());
        assertEquals(new BigDecimal("1000"), auction.getMinBidIncrement());
        assertNotNull(auction.getBidHistory());
    }

    @Test
    public void testSettersAndGetters() {
        Auction auction = new Auction();
        BigDecimal price = new BigDecimal("75000");
        BigDecimal reserve = new BigDecimal("100000");
        BigDecimal increment = new BigDecimal("2000");
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusMinutes(30);

        auction.setItemId(10);
        auction.setSellerId(20);
        auction.setHighestBidderId(30);
        auction.setStartingPrice(price);
        auction.setCurrentPrice(price);
        auction.setReservePrice(reserve);
        auction.setMinBidIncrement(increment);
        auction.setStatus(AuctionStatus.FINISHED);
        auction.setExtensionCount(2);
        auction.setTotalBids(5);

        List<BidTransaction> history = new ArrayList<>();
        auction.setBidHistory(history);

        assertEquals(10, auction.getItemId());
        assertEquals(20, auction.getSellerId());
        assertEquals(30, auction.getHighestBidderId());
        assertEquals(price, auction.getStartingPrice());
        assertEquals(price, auction.getCurrentPrice());
        assertEquals(reserve, auction.getReservePrice());
        assertEquals(increment, auction.getMinBidIncrement());
        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
        assertEquals(2, auction.getExtensionCount());
        assertEquals(5, auction.getTotalBids());
        assertSame(history, auction.getBidHistory());
    }
}
