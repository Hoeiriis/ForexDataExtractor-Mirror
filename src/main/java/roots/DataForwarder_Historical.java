package roots;

import com.dukascopy.api.*;
import com.dukascopy.api.feed.IFeedDescriptor;
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
import java.time.LocalDate;
import java.util.*;


public class DataForwarder_Historical extends DataForwarder {

    private DataCollector theCollector;
    private Map<UUID, String> featureDescription;
    private List<Map<UUID, Double[]>> featureCollection;
    private SnapshotTarget timeSnapshot;
    private UUID timestampId;
    public String savePath;
    private String outPath;
    private int file_counter;
    private int targetRange;
    private SnapshotTarget targetSnapshot;


    public DataForwarder_Historical(List<SubscriptionWindowFeed> subscriptionWindowFeeds,
                                    List<SubscriptionWindowIndicator> subscriptionWindowIndicators,
                                    IFeedDescriptor feedDescriptor, int targetMin, String outputPath)
    {
        super(subscriptionWindowFeeds, subscriptionWindowIndicators, feedDescriptor);


        featureCollection = new ArrayList<>();

        theCollector = new DataCollector(false);
        theCollector.autoSubscribe(this.subscriptionWindowFeeds.toArray(new SubscriptionWindow[0]));
        theCollector.autoSubscribe(this.subscriptionWindowIndicators.toArray(new SubscriptionWindow[0]));

        targetRange = targetMin;
        targetSnapshot = new SnapshotTarget(UUID.randomUUID(), "Target");

        outPath = outputPath;
        timestampId = UUID.randomUUID();
        timeSnapshot = new SnapshotTarget(timestampId, "Datetime");
        System.out.print("Constructor called \n");
        current_week = 0;
        weekCount = 10;
        file_counter = 1;


    }

    @Override
    public void onStart(IContext context) throws JFException
    {

        history = context.getHistory();
        context.subscribeToFeed(feed, this);

        IIndicators indicators = context.getIndicators();
        ITimedData latestFeedData = history.getFeedData(feed, 1);
        var bar15back = getBarsNMinutesBack((IBar) latestFeedData, targetRange).get(0);
        acquireRecentHistory(bar15back, indicators);
    }

    @Override
    public void onFeedData(IFeedDescriptor feedDescriptor, ITimedData feedData)
    {
        try {
            // Get data from 15 minutes ago
            List<IBar> bars15back = getBarsNMinutesBack((IBar) feedData, targetRange); //Old data config

            // Return if weekend
            if (bars15back.size() == 0){
                return;
            }

            SimpleDateFormat simpleDateformat = new SimpleDateFormat("E");
            String dayOfWeek = simpleDateformat.format(new Date(bars15back.get(0).getTime()));
            if(dayOfWeek.equalsIgnoreCase("Sun")){
                return;
            }

            // Feed data to data windows
            FeedDataWindows(feedDescriptor, bars15back);

            // Get features
            var features = theCollector.getFeatureCollection();
            featureDescription = theCollector.getFeatureDescription();

            // If all data is present, write to file
            if(featureCollection.size() > 0 && featureCollection.get(0).size() != featureDescription.size()) {
                featureCollection = new ArrayList<>();
            }

            if (featureCollection.size() > 0 && featureCollection.size() % 100 == 0){
                // Check the week, if a 4 week period has passed, change the out file
                CheckWeek(bars15back.get(0).getTime());
                writeToFile(savePath);
                featureCollection = new ArrayList<>();
            }

            featureCollection.add(features);

        } catch (Exception e) {
            System.out.print(e.getMessage());
            e.printStackTrace();

        }
    }

    private List<IBar> getBarsNMinutesBack(IBar currentBar, int minutesBack) throws JFException {
        long startTime =  history.getTimeForNBarsBack(Period.ONE_MIN, currentBar.getTime(), minutesBack);
        return history.getBars(Instrument.EURUSD, Period.ONE_MIN, OfferSide.ASK, Filter.WEEKENDS, startTime, currentBar.getTime());
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

            var size = featureCol.get(key).length;;
            //if(featureDescription.get(key).equals("Target") | featureDescription.get(key).equals("feed15min_") | featureDescription.get(key).equals("feed5min_")){
            //    size = featureCol.get(key).length;
            //}

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

                int samples = features.length;
                //if(featureDescription.get(key).equals("Target") | featureDescription.get(key).equals("feed15min_") | featureDescription.get(key).equals("feed5min_")){
                //    samples = features.length;
                //}

                for (var entry : features){

                    if(samples < 1){
                        break;
                    }

                    featuresStrings.add(entry.toString());
                    samples = samples-1;
                }
            }
            dataAsString.add(featuresStrings.toArray(new String[]{}));
        }

        return dataAsString;
    }

    private void writeToCSV(List<String[]> data,  String FilePath) throws Exception{
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
    }

    private void FeedDataWindows(IFeedDescriptor feedDescriptor, List<IBar> barData) throws Exception {
        // Feed the different data windows

        // Compute target
        targetSnapshot.setWindow(computeTargetRange(barData));

        // Feed the other windows
        var bar15back = barData.get(0);
        feedWindowFeeds(feedDescriptor, bar15back);
        feedWindowIndicators(feedDescriptor, bar15back);

        // Set time and invoke NewSnapshot for time and target
        var tstamp = (double) bar15back.getTime();
        timeSnapshot.setWindow((new Double[]{tstamp}));
        theCollector.NewSnapshot(timeSnapshot);
        theCollector.NewSnapshot(targetSnapshot);
    }


    private int weekCount;
    private int current_week;

    private void CheckWeek(long msTime){
        Date date = new Date(msTime);
        int week_of_year = Integer.parseInt(new SimpleDateFormat("w").format(date));
        if(current_week != week_of_year){

            weekCount += 1;
            current_week = week_of_year;

            if(weekCount > 3){
                savePath = String.format("%s%d_4weeks_%.10s.csv", outPath, file_counter, date.toString());
                weekCount = 1;
                file_counter += 1;
            }

        }
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
}