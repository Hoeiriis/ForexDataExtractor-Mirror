package roots;

import com.dukascopy.api.*;
import com.dukascopy.api.feed.IFeedDescriptor;
import com.dukascopy.api.feed.IFeedListener;
import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;
import com.opencsv.bean.OpencsvUtils;
import roots.SubWindows.SubscriptionWindow;
import roots.SubWindows.SubscriptionWindowFeed;
import roots.SubWindows.SubscriptionWindowIndicator;

import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static roots.ThingsThatShouldBeEasyInJavaButAreNot.flatten2DDoubleArray;

public class DataForwarder_Historical implements IStrategy, IFeedListener {

    public List<SubscriptionWindowFeed> subscriptionWindowFeeds;
    public List<SubscriptionWindowIndicator> subscriptionWindowIndicators;
    private IHistory history;
    private DataCollector theCollector;
    private Map<UUID, String> featureDescription;
    private List<Map<UUID, Double[][]>> featureCollection;
    private int targetRange;


    public DataForwarder_Historical()
    {
        SubscriptionInitializer initializer = new SubscriptionInitializer();

        /* Initializing components */
        this.subscriptionWindowFeeds = initializer.InitSubscriptionWindowFeeds();
        this.subscriptionWindowIndicators = initializer.InitSubscriptionWindowIndicators();

        theCollector = new DataCollector(false);
        theCollector.autoSubscribe(this.subscriptionWindowFeeds.toArray(new SubscriptionWindow[0]));
        theCollector.autoSubscribe(this.subscriptionWindowIndicators.toArray(new SubscriptionWindow[0]));
        featureCollection = new ArrayList<>();
    }

    @Override
    public void onStart(IContext context) throws JFException
    {
        history = context.getHistory();
        var descriptorsToSubscribe = new ArrayList<IFeedDescriptor>();

        // Initialize and acquire data for FeedWindows
        for (SubscriptionWindowFeed subWin : subscriptionWindowFeeds)
        {
            IFeedDescriptor feedDescriptor = subWin.getFeedDescriptor();

            // Add if not already subscribed
            if (!descriptorsToSubscribe.contains(feedDescriptor)){
                descriptorsToSubscribe.add(feedDescriptor);
            }

            // Set history
            ITimedData lastFeedData = history.getFeedData(feedDescriptor, 1);
            IBar bar15back = getBarsNMinutesBack((IBar) lastFeedData, 15).get(0);
            List<ITimedData> feedDataList = history.getFeedData(feedDescriptor, subWin.LookBackRange, bar15back.getTime(), 0);
            subWin.setWindow((IBar[]) feedDataList.toArray(new IBar[0]));

            try {
                subWin.notifySubscribers();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.print(e.getMessage());
            }
        }


        // Initialize and acquire information for IndicatorWindows
        IIndicators indicators = context.getIndicators();
        for (SubscriptionWindowIndicator subWinInd : subscriptionWindowIndicators){

            var indFeedDescriptor = subWinInd.getFeedDescriptor();

            if (!descriptorsToSubscribe.contains(indFeedDescriptor)){
                descriptorsToSubscribe.add(indFeedDescriptor);
            }
            subWinInd.setIndicators(indicators);
            ITimedData lastFeedData = history.getFeedData(indFeedDescriptor, 1);
            IBar bar15back = getBarsNMinutesBack((IBar) lastFeedData, 15).get(0);

            try {
                subWinInd.pushToIndicator(bar15back);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Subscribe to all feeds present
        for (var feed : descriptorsToSubscribe){
            context.subscribeToFeed(feed, this);
        }
    }

    @Override
    public void onFeedData(IFeedDescriptor feedDescriptor, ITimedData feedData)
    {
        try {
            // Get data from 15 minutes ago
            List<IBar> bars15back = getBarsNMinutesBack((IBar) feedData, 15);

            bars15back.get(0).

            // TODO: Also get target value/values
            feedWindowFeeds(feedDescriptor, bars15back.get(0));
            feedWindowIndicators(feedDescriptor, bars15back.get(0));

            var features = theCollector.getFeatureCollection();
            featureDescription = theCollector.getFeatureDescription();

            if(featureCollection.size() > 0 && featureCollection.get(0).size()+targetRange != featureDescription.size()) {
                featureCollection = new ArrayList<>();
            }

            if (featureCollection.size() > 0 && featureCollection.size() % 6 == 0){
                var stringData = convertToStrings();
                writeToCSV(stringData, "/home/obliviousmonkey/CoreView/WhatYaWannaKnow/IceRoot_Output_Data/test3.csv");
                System.out.print("printed \n");
            }

            featureCollection.add(features);
            System.out.print(featureCollection.size()+"\n");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.print(e.getMessage());
        }
    }

    private void feedWindowFeeds(IFeedDescriptor feedDescriptor, IBar barData){

        for (SubscriptionWindowFeed w : subscriptionWindowFeeds) {
            if (feedDescriptor.equals(w.FeedDescriptor)) {
                try {
                    w.pushToWindow(barData);
                } catch (Exception e) {
                    System.out.print(String.format("exceptiuones: %s\n", e.toString()));
                }
            }
        }
    }

    private void feedWindowIndicators(IFeedDescriptor feedDescriptor, IBar barData){

        for (SubscriptionWindowIndicator w : subscriptionWindowIndicators) {
            if (feedDescriptor.equals(w.getFeedDescriptor())) {
                try {
                    w.pushToIndicator(barData);
                } catch (Exception e) {
                    System.out.print(String.format("exceptiuones: %s\n", e.toString()));
                }
            }
        }
    }

    private List<IBar> getBarsNMinutesBack(IBar currentBar, int minutesBack) throws JFException {
        long startTime =  history.getTimeForNBarsBack(Period.ONE_MIN, currentBar.getTime(), minutesBack);
        List<IBar> bars = history.getBars(Instrument.EURUSD, Period.ONE_MIN, OfferSide.ASK, startTime, currentBar.getTime());
        return bars;
    }

    private List<String> createDescription() {

        var keys = featureDescription.keySet();

        List<String> descriptions = new ArrayList<>();
        var featureCol = featureCollection.get(0);

        for (var key : keys) {
            var values = featureCol.get(key);
            var size = flatten2DDoubleArray(values).length;

            var desc = featureDescription.get(key);
            for (int i = 0; i < size; i++) {
                descriptions.add(desc + i);
            }
        }

        return descriptions;
    }


    private List<String[]> convertToStrings(){

        List<String[]> dataAsString = new ArrayList<>();
        var keys = featureDescription.keySet();

        for (var featureSet : featureCollection ){
            List<String> featuresStrings = new ArrayList<>();
            for (var key : keys) {
                var doubleMatrix = featureSet.get(key);
                var flattened = flatten2DDoubleArray(doubleMatrix);

                for (var entry : flattened){
                    featuresStrings.add(entry.toString());
                }
            }

            dataAsString.add(featuresStrings.toArray(new String[]{}));
        }

        return dataAsString;
    }

    public void writeToCSV(List<String[]> data,  String FilePath) throws Exception{
        Writer writer;
        File f = new File(FilePath);
        boolean writeDesc = false;

        if(f.exists()) {
            writer = new FileWriter(FilePath, true);
        }
        else{
            writer = new FileWriter(FilePath);
            writeDesc = true;
        }

        CSVWriter csvWriter = new CSVWriter(writer, ',', '\0', ICSVWriter.DEFAULT_ESCAPE_CHARACTER, ICSVWriter.DEFAULT_LINE_END);

        if(writeDesc){
            var desc = createDescription();
            csvWriter.writeNext(desc.toArray(new String[]{}));
        }

        csvWriter.writeAll(data);
        csvWriter.close();
    }


    //region non-used onX

    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {

    }

    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {

    }

    @Override
    public void onMessage(IMessage message) throws JFException {

    }

    @Override
    public void onAccount(IAccount account) throws JFException {

    }

    @Override
    public void onStop() throws JFException {
    }

    //endregion
}