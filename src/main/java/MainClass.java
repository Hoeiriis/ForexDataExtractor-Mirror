import com.dukascopy.api.*;
import com.dukascopy.api.feed.FeedDescriptor;
import com.dukascopy.api.feed.util.TimePeriodAggregationFeedDescriptor;
import roots.DataCollector;
import roots.DataForwarder;
import roots.SubWindows.SubscriptionWindow;
import roots.SubWindows.SubscriptionWindowFeed;
import roots.SubWindows.SubscriptionWindowIndicator;
import roots.SubscriptionInitializer;

import java.util.List;

public class MainClass {

    public static void main(String [] args) throws Exception {

        String password = "DfNED";
        String userName = "DEMO2DfNED";

        SubscriptionInitializer initializer = new SubscriptionInitializer();

        /* Initializing components */

        List<SubscriptionWindowFeed> subWindows = initializer.InitSubscriptionWindowFeeds();
        List<SubscriptionWindowIndicator> indicatorWindows = initializer.InitSubscriptionWindowIndicators(1);

        DataCollector theCollector = new DataCollector(true);
        theCollector.autoSubscribe(subWindows.toArray(new SubscriptionWindow[0]));
        theCollector.autoSubscribe(indicatorWindows.toArray(new SubscriptionWindow[0]));


        DataStreamConnector Conn = new DataStreamConnector(userName, password, false);
        Conn.subscribeToInstrument(Instrument.EURUSD);
        DataForwarder forwarder = new DataForwarder(subWindows, indicatorWindows, new TimePeriodAggregationFeedDescriptor(Instrument.EURUSD, Period.ONE_MIN, OfferSide.ASK));

        /* Starting */
        Conn.startStrategy(forwarder);

    }
}
