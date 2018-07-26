package info.blockchain.wallet.shapeshift;

import info.blockchain.wallet.shapeshift.data.Quote;
import info.blockchain.wallet.shapeshift.data.Trade;
import info.blockchain.wallet.shapeshift.data.Trade.STATUS;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class ShapeShiftTradesTest {

    private ShapeShiftTrades subject = new ShapeShiftTrades();

    @Test
    public void constructor1() throws Exception {

        //Arrange

        //Act
        subject = new ShapeShiftTrades();

        //Assert
        Assert.assertEquals(0, subject.getTrades().size());
    }

    @Test
    public void load_trade_status_received() throws Exception {

        //Arrange
        Quote quote = new Quote();
        quote.setOrderId("order id");
        quote.setDeposit("deposit");
        quote.setExpiration(5000);
        quote.setQuotedRate(BigDecimal.TEN);
        quote.setMinerFee(BigDecimal.ONE);
        quote.setWithdrawal("withdrawal");
        quote.setWithdrawalAmount(BigDecimal.TEN);

        List<Trade> trades = new ArrayList<>();
        Trade trade1 = new Trade();
        trade1.setQuote(quote);
        trade1.setHashIn("hash in");
        trade1.setHashOut("hash out");
        trade1.setStatus(STATUS.RECEIVED);
        trade1.setTimestamp(1000);
        trades.add(trade1);


        ShapeShiftTrades ssData = new ShapeShiftTrades();
        ssData.setTrades(trades);

        String response = "{\"payload\":\"uasmFuo40Q4PrAozy+Uswx1oBry2IjUtzmuCkKRS+VUtAFGp9EIL5Rejf88wqXwKQ2h7Mf2X9YF9g0+vwvwQKb5f+r/QIbIHjMnGFkQheRueljYHwYjTDOC7GmqU2ClCdC4nWjrnbbvMa8Mbgms2UgePHO4ncgr/yRVHWTRO43OZOF7N6pjakXuS0Lq1cZKL6pb0B+Mi1tWii15P8Q6FMIDqALPpl75pRpz3mMQlOA9UmZtLwOZQJa4BKGkV3OD3IKU+S5F5Zph9L/KtZyJZK8pjbTmCmu13VsXazRO3SRIA7HEUya2cS9dmyys/xn078bJEM7MDrYS50+IOwLK1Onb1Dgcmb79faPvY3J29Y+Uq9b6JGKhxpzcr/JBKKcMULSrNAIR1SSkX6ilc3Ntg1fNyKR17KznW8z+jqwy24vKAKRinpl1n6qvIVhaG/QU8ikCLbSdJLHlR1wFF9kXaXwVy0E7RryxqJB9slxe0v9JA59qHeNrZLA66s9/GHXazOScy9iKv3GIMpoiZNaOvtGxYbW6PN2ayjlvqIpOxFE0Ie8AGsoQNdFZb1pKYKTZDWdb/Wj3s8ppG6Ffxz8BfdNTBDR9kabMDU1l4pZ4lXoM=\",\"version\":1,\"type_id\":6,\"signature\":\"H1w0MzoOlvkBl3vU+ROqpBEDJbt56QsKKmgy1TMOaAgTSjlD/BRnWfuwC07cmkRI66HyIqcncXhFkjIxADudeWg=\",\"prev_magic_hash\":\"54f17f58823da7db98c7c01dace66ade6d1792c20c612f16cc36c8abe53c0872\",\"address\":\"16frGZdJNT1APT9ht6m91FKXQSgGUr91jy\",\"created_at\":1507724873000,\"updated_at\":1507724896000}";

        //Act
        subject = ShapeShiftTrades.load(ssData.toJson());

        //Assert
        Assert.assertEquals(1, subject.getTrades().size());
        Trade trade = subject.getTrades().get(0);
        Assert.assertEquals(STATUS.RECEIVED, trade.getStatus());
        Assert.assertEquals("hash in", trade.getHashIn());
        Assert.assertEquals(1000, trade.getTimestamp());
        Assert.assertEquals("order id", trade.getQuote().getOrderId());
        Assert.assertEquals("deposit", trade.getQuote().getDeposit());
        Assert.assertEquals(5000, trade.getQuote().getExpiration());
        Assert.assertEquals(BigDecimal.TEN, trade.getQuote().getQuotedRate());
        Assert.assertEquals(BigDecimal.ONE, trade.getQuote().getMinerFee());
        Assert.assertEquals("withdrawal", trade.getQuote().getWithdrawal());
        Assert.assertEquals(BigDecimal.TEN, trade.getQuote().getWithdrawalAmount());
    }

    @Test
    public void load_null() throws Exception {

        //Arrange
        String response = null;

        //Act
        subject = ShapeShiftTrades.load(response);

        //Assert
        Assert.assertNull(subject);
    }
}