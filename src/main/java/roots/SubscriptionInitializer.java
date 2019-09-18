package roots;

import com.dukascopy.api.*;
import com.dukascopy.api.feed.FeedDescriptor;
import com.dukascopy.api.feed.util.TimePeriodAggregationFeedDescriptor;
import roots.SubWindows.Indicators.BollingerBandsWindow;
import roots.SubWindows.Indicators.EMAWindow;
import roots.SubWindows.Indicators.RSIWindow;
import roots.SubWindows.Indicators.SMAWindow;
import roots.SubWindows.SubscriptionWindowFeed;
import roots.SubWindows.SubscriptionWindowIndicator;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionInitializer {

    public List<SubscriptionWindowFeed> InitSubscriptionWindowFeeds(){

        List<SubscriptionWindowFeed> returnList = new ArrayList<>();

        // 1 min Bar, 30 min Lookback, Price/Volume
        FeedDescriptor feed1= new TimePeriodAggregationFeedDescriptor(Instrument.EURUSD, Period.ONE_MIN, OfferSide.ASK, Filter.WEEKENDS);
        SubscriptionWindowFeed window1 = new SubscriptionWindowFeed(feed1, 30, "feed1min_");
        returnList.add(window1);

        // 5 min Bar, 60 min Lookback, Price/Volume
        FeedDescriptor feed2 = new TimePeriodAggregationFeedDescriptor(Instrument.EURUSD, Period.FIVE_MINS, OfferSide.ASK, Filter.WEEKENDS);
        SubscriptionWindowFeed window2 = new SubscriptionWindowFeed(feed2, 12, "feed5mins_");
        returnList.add(window2);

        // 15 min Bar, 4 hours Lookback, Price/Volume
        FeedDescriptor feed3 = new TimePeriodAggregationFeedDescriptor(Instrument.EURUSD, Period.FIFTEEN_MINS, OfferSide.ASK, Filter.WEEKENDS);
        SubscriptionWindowFeed window3 = new SubscriptionWindowFeed(feed3, 16, "feed15min_");
        returnList.add(window3);

        return returnList;

    }

    public List<SubscriptionWindowIndicator> InitSubscriptionWindowIndicators(){
        List<SubscriptionWindowIndicator> returnList = new ArrayList<>();

        // All indicators use same feed in this case
        FeedDescriptor indicator_feed = new TimePeriodAggregationFeedDescriptor(Instrument.EURUSD, Period.ONE_MIN, OfferSide.ASK, Filter.WEEKENDS);
        IIndicators.AppliedPrice appliedPrice = IIndicators.AppliedPrice.CLOSE;

        // SMA1, 1 min bar, 15 min lookback, moving average 20
        SMAWindow sma1 = new SMAWindow(indicator_feed, 15, appliedPrice, 20, "SMA1_");
        returnList.add(sma1);

        // SMA2, 1 min bar, 15 min lookback, moving average 50
        SMAWindow sma2 = new SMAWindow(indicator_feed, 15, appliedPrice, 50, "SMA2_");
        returnList.add(sma2);

        // EMA, 1 min bar, 15 min lookback, moving average 20
        EMAWindow ema1 = new EMAWindow(indicator_feed, 15, appliedPrice, 20, "EMA1_");
        returnList.add(ema1);

        //Bollinger Bands, 1 min bar, 15 min lookback, std_dev 2, MA type EMA 50
        BollingerBandsWindow bbands1 = new BollingerBandsWindow(indicator_feed, 15, appliedPrice, 50, 2, IIndicators.MaType.EMA, "BB1_");
        returnList.add(bbands1);

        //RSI, 1 min bar, 15 min lookback, RSI lookback 14
        RSIWindow rsi1 = new RSIWindow(indicator_feed, 15, appliedPrice, 14, "RSI1_");
        returnList.add(rsi1);

        return returnList;
    }

}
