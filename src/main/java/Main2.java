import com.dukascopy.api.*;
import com.dukascopy.api.feed.FeedDescriptor;
import com.dukascopy.api.feed.util.TimePeriodAggregationFeedDescriptor;
import roots.DataForwarder_Historical;
import roots.SubWindows.SubscriptionWindowFeed;
import roots.SubWindows.SubscriptionWindowIndicator;
import roots.SubscriptionInitializer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


public class Main2 {

    public static void main(String [] args) throws Exception {

        String password = "jbCoU";
        String userName = "DEMO2jbCoU";
        String savePath = "/home/obliviousmonkey/CoreView/WhatYaWannaKnow/IceRoot_Output_Data/test/";
        //String savePath = "/home/happysun/data/dukascopy/weeks/";

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        //dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+1"));

        Date dateFrom = dateFormat.parse("2017/12/01 00:00:00");
        Date dateTo = dateFormat.parse("2018/02/28 00:00:00");

        System.out.println("Start date: "+dateFrom);
        System.out.println("End date: "+dateTo);

        /* Initializing components */
        System.out.println("Initializing components");
        SubscriptionInitializer initializer = new SubscriptionInitializer();
        List<SubscriptionWindowFeed> subWindows = initializer.InitSubscriptionWindowFeeds();
        List<SubscriptionWindowIndicator> indicatorWindows = initializer.InitSubscriptionWindowIndicators();
        FeedDescriptor feed = new TimePeriodAggregationFeedDescriptor(Instrument.EURUSD, Period.ONE_MIN, OfferSide.ASK, Filter.WEEKENDS);

        DataForwarder_Historical forwarder = new DataForwarder_Historical(subWindows, indicatorWindows, feed, 15, savePath);

        // Using Historical connector
        System.out.println("Connecting to stream");
        HistoricalStreamConnector Conn = new HistoricalStreamConnector(userName, password);

        System.out.println("Subscribing to instruments");
        Conn.subscribeToInstrument(Instrument.EURUSD);

        /* Starting */
        System.out.println("Starting historical DataForwarder");
        Conn.startStrategy(forwarder, dateFrom, dateTo);
    }
}