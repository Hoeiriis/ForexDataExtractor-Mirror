package roots;

import com.dukascopy.api.*;
import com.dukascopy.api.feed.FeedDescriptor;
import com.dukascopy.api.feed.IFeedDescriptor;
import com.dukascopy.api.feed.IFeedListener;
import com.dukascopy.api.feed.util.TimePeriodAggregationFeedDescriptor;
import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;
import roots.Snapshots.SnapshotTarget;
import roots.SubWindows.SubscriptionWindow;
import roots.SubWindows.SubscriptionWindowFeed;
import roots.SubWindows.SubscriptionWindowIndicator;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.*;


public class DataForwarder_Historical implements IStrategy, IFeedListener {

    public List<SubscriptionWindowFeed> subscriptionWindowFeeds;
    public List<SubscriptionWindowIndicator> subscriptionWindowIndicators;
    private IHistory history;
    private DataCollector theCollector;
    private Map<UUID, String> featureDescription;
    private List<Map<UUID, Double[]>> featureCollection;
    private int targetRange;
    private SnapshotTarget targetSnapshot;
    private SnapshotTarget timeSnapshot;
    private UUID timestampId;
    public String savePath;


    public DataForwarder_Historical(String outputPath)
    {
        savePath = outputPath;
        SubscriptionInitializer initializer = new SubscriptionInitializer();

        /* Initializing components */
        this.subscriptionWindowFeeds = initializer.InitSubscriptionWindowFeeds();
        this.subscriptionWindowIndicators = initializer.InitSubscriptionWindowIndicators();

        theCollector = new DataCollector(false);
        theCollector.autoSubscribe(this.subscriptionWindowFeeds.toArray(new SubscriptionWindow[0]));
        theCollector.autoSubscribe(this.subscriptionWindowIndicators.toArray(new SubscriptionWindow[0]));
        featureCollection = new ArrayList<>();
        targetRange = 15;
        targetSnapshot = new SnapshotTarget(UUID.randomUUID(), "Target");
        timestampId = UUID.randomUUID();
        timeSnapshot = new SnapshotTarget(timestampId, "Datetime");
        System.out.print("Constructor called \n");
    }

    @Override
    public void onStart(IContext context) throws JFException
    {
        System.out.print("onStart Called \n");
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
            List<IBar> feedData = getBarsWithNPeriod(feedDescriptor.getPeriod(), subWin.LookBackRange, bar15back.getTime());
            subWin.setWindow(feedData.toArray(new IBar[0]));

            try {
                subWin.notifySubscribers();
            } catch (Exception e) {
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
            System.out.print("feedData\n");
            // Get data from 15 minutes ago
            List<IBar> bars15back = getBarsNMinutesBack((IBar) feedData, targetRange);

            feedWindowFeeds(feedDescriptor, bars15back.get(0));
            feedWindowIndicators(feedDescriptor, bars15back.get(0));
            targetSnapshot.setWindow(computeTargetRange(bars15back));
            var tstamp = (double) bars15back.get(0).getTime();
            timeSnapshot.setWindow((new Double[]{tstamp}));
            theCollector.NewSnapshot(timeSnapshot);
            theCollector.NewSnapshot(targetSnapshot);

            var features = theCollector.getFeatureCollection();
            featureDescription = theCollector.getFeatureDescription();

            if(featureCollection.size() > 0 && featureCollection.get(0).size() != featureDescription.size()) {
                featureCollection = new ArrayList<>();
            }

            if (featureCollection.size() > 0 && featureCollection.size() % 10 == 0){
                writeToFile(savePath);
                featureCollection = new ArrayList<>();
            }

            featureCollection.add(features);
            System.out.print(featureCollection.size()+"\n");

        } catch (Exception e) {
            System.out.print(e.getMessage());
            e.printStackTrace();
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
        return history.getBars(Instrument.EURUSD, Period.ONE_MIN, OfferSide.ASK, startTime, currentBar.getTime());
    }

    private List<IBar> getBarsWithNPeriod(Period period, int nBarsToGet, long barTime) throws JFException {

        var returnBars = new ArrayList<IBar>();
        FeedDescriptor oneMin= new TimePeriodAggregationFeedDescriptor(Instrument.EURUSD, Period.ONE_MIN, OfferSide.ASK);

        // If period is one minutes, call standard Dukascopy API
        if(period.getNumOfUnits() == 1){
            var bars = history.getFeedData(oneMin, nBarsToGet, barTime, 0);
            for (var bar : bars){
                returnBars.add((IBar) bar);
            }

            return returnBars;
        }

        // Retrieve all OneMin bars for the whole time period
        int nPeriod = period.getNumOfUnits();
        int totalBars = nPeriod*(nBarsToGet+1);
        List<ITimedData> data = history.getFeedData(oneMin, totalBars, barTime, 0);

        // Construct new IBars for each period in the period
        IBar bar1 = (IBar) data.get(0);
        Bar buildBar = new Bar(bar1.getOpen(), 0, bar1.getLow(), bar1.getHigh(), bar1.getVolume(), bar1.getTime());
        for(var i = 1; i < data.size(); i++){

            var currBar = (IBar) data.get(i);
            buildBar.updateHigh(currBar.getHigh());
            buildBar.updateLow(currBar.getLow());
            buildBar.appendVolume(currBar.getVolume());

            if (i % nPeriod == 0){
                buildBar.close = currBar.getClose();
                returnBars.add(buildBar);

                var nextBar = (IBar) data.get(i+1);
                buildBar = new Bar(nextBar.getOpen(), 0, 1000000, 0, 0, nextBar.getTime());
            }
        }

        return returnBars;
    }


    private Double[] computeTargetRange(List<IBar> targetBars){
        List<Double> targetVals = new ArrayList<>();
        for(var bar : targetBars){
            targetVals.add((bar.getOpen()+bar.getHigh()+bar.getLow()+bar.getClose())/4);
        }

        return targetVals.toArray(new Double[]{});
    }

    private List<String> createDescription() {

        var keys = featureDescription.keySet();

        List<String> descriptions = new ArrayList<>();
        var featureCol = featureCollection.get(0);
        descriptions.add(featureDescription.get(timestampId));

        for (var key : keys) {

            if(key == timestampId){ continue; }

            var size = featureCol.get(key).length;

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
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

        for (var featureSet : featureCollection ){
            List<String> featuresStrings = new ArrayList<>();
            Long timestampLong = featureSet.get(timestampId)[0].longValue();


            var timestampDate = new Date(timestampLong);
            var dateString = format.format(timestampDate);
            featuresStrings.add(dateString);

            for (var key : keys) {

                if(key == timestampId){ continue; }
                var features = featureSet.get(key);

                for (var entry : features){
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

    private void writeToFile(String savePath) throws Exception {
        var stringData = convertToStrings();
        writeToCSV(stringData, savePath);
        System.out.print("printed \n");
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
        try {
            writeToFile(savePath);
            featureCollection = new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //endregion
}