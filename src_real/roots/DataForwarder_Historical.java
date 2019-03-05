package roots;

import com.dukascopy.api.*;
import com.dukascopy.api.feed.IFeedDescriptor;
import com.dukascopy.api.feed.IFeedListener;
import com.opencsv.CSVWriter;
import roots.SubWindows.SubscriptionWindow;
import roots.SubWindows.SubscriptionWindowFeed;
import roots.SubWindows.SubscriptionWindowIndicator;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.List;

public class DataForwarder_Historical implements IStrategy, IFeedListener {

    public List<SubscriptionWindowFeed> subscriptionWindowFeeds;
    public List<SubscriptionWindowIndicator> subscriptionWindowIndicators;
    private IHistory history;
    private DataCollector theCollector;


    public DataForwarder_Historical()
    {
        SubscriptionInitializer initializer = new SubscriptionInitializer();

        /* Initializing components */
        this.subscriptionWindowFeeds = initializer.InitSubscriptionWindowFeeds();
        this.subscriptionWindowIndicators = initializer.InitSubscriptionWindowIndicators();

        theCollector = new DataCollector(false);
        theCollector.autoSubscribe(this.subscriptionWindowFeeds.toArray(new SubscriptionWindow[0]));
        theCollector.autoSubscribe(this.subscriptionWindowIndicators.toArray(new SubscriptionWindow[0]));
    }

    @Override
    public void onStart(IContext context) throws JFException
    {
        history = context.getHistory();

        for (SubscriptionWindowFeed subWin : subscriptionWindowFeeds)
        {
            IFeedDescriptor feedDescriptor = subWin.getFeedDescriptor();
            context.subscribeToFeed(feedDescriptor, this);

            // Set history
            ITimedData lastFeedData = history.getFeedData(feedDescriptor, 0);
            List<ITimedData> feedDataList = history.getFeedData(feedDescriptor, subWin.LookBackRange, lastFeedData.getTime(), 0);
            subWin.setWindow(feedDataList.toArray(new IBar[0]));

            try {
                subWin.notifySubscribers();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        IIndicators indicators = context.getIndicators();

        for (SubscriptionWindowIndicator subWinInd : subscriptionWindowIndicators){
            context.subscribeToFeed(subWinInd.getFeedDescriptor(), this);
            subWinInd.setIndicators(indicators);
            ITimedData lastFeedData = history.getFeedData(subWinInd.getFeedDescriptor(), 0);

            try {
                subWinInd.pushToIndicator((IBar) lastFeedData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onFeedData(IFeedDescriptor feedDescriptor, ITimedData feedData)
    {
        try {
            IBar bar15back = getBar15MinutesBack((IBar) feedData);

            feedWindowFeeds(feedDescriptor, bar15back);
            feedWindowIndicators(feedDescriptor, bar15back);
        } catch (JFException e) {
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

    private IBar getBar15MinutesBack(IBar currentBar) throws JFException {
        long startTime =  history.getTimeForNBarsBack(Period.ONE_MIN, currentBar.getTime(), 15);
        List<IBar> bars = history.getBars(Instrument.EURUSD, Period.ONE_MIN, OfferSide.ASK, startTime, currentBar.getTime());
        return bars.get(0);
    }


    public void writeToCSV(List<String[]> data,  String FilePath) throws Exception{
        Writer writer;
        File f = new File(FilePath);

        if(f.exists()) {
            writer = new FileWriter(FilePath, true);
        }
        else{
            writer = new FileWriter(FilePath);
        }

        CSVWriter csvWriter = new CSVWriter(writer, ',', '\0');
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