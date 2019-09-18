package roots;

import com.dukascopy.api.*;
import com.dukascopy.api.feed.FeedDescriptor;
import com.dukascopy.api.feed.IFeedDescriptor;
import com.dukascopy.api.feed.IFeedListener;
import com.dukascopy.api.feed.util.TimePeriodAggregationFeedDescriptor;
import roots.SubWindows.SubscriptionWindowFeed;
import com.dukascopy.api.IIndicators;
import roots.SubWindows.SubscriptionWindowIndicator;

import java.util.ArrayList;
import java.util.List;

public class DataForwarder implements IStrategy, IFeedListener {

    public List<SubscriptionWindowFeed> subscriptionWindowFeeds;
    public List<SubscriptionWindowIndicator> subscriptionWindowIndicators;
    public IFeedDescriptor feed;
    protected IHistory history;


    public DataForwarder(List<SubscriptionWindowFeed> subscriptionWindowFeeds, List<SubscriptionWindowIndicator> subscriptionWindowIndicators, IFeedDescriptor feedDescriptor){
        this.subscriptionWindowFeeds = subscriptionWindowFeeds;
        this.subscriptionWindowIndicators = subscriptionWindowIndicators;
        this.feed = feedDescriptor;
    }

    @Override
    public void onStart(IContext context) throws JFException 
    {
        history = context.getHistory();
        context.subscribeToFeed(feed, this);

        IIndicators indicators = context.getIndicators();
        ITimedData latestFeedData = history.getFeedData(feed, 1);
        acquireRecentHistory((IBar) latestFeedData, indicators);
    }

    protected void acquireRecentHistory(IBar latestFeedData, IIndicators indicators) throws JFException {

        for (SubscriptionWindowFeed subWin : subscriptionWindowFeeds)
        {
            // Set history
            List<IBar> feedData = getBarsWithNPeriod(feed.getPeriod(), subWin.LookBackRange, latestFeedData.getTime());
            subWin.setWindow(feedData.toArray(new IBar[0]));

            try {
                subWin.notifySubscribers();
            } catch (Exception e) {
                System.out.print(e.getMessage());
            }
        }

        // Initialize and acquire information for IndicatorWindows
        for (SubscriptionWindowIndicator subWinInd : subscriptionWindowIndicators){
            subWinInd.setIndicators(indicators);

            try {
                subWinInd.pushToIndicator((IBar) latestFeedData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected List<IBar> getBarsWithNPeriod(Period period, int nBarsToGet, long barTime) throws JFException {

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

    @Override
    public void onFeedData(IFeedDescriptor feedDescriptor, ITimedData feedData)
    {
        feedWindowFeeds(feedDescriptor, feedData);
        feedWindowIndicators(feedDescriptor, feedData);
    }

    protected void feedWindowFeeds(IFeedDescriptor feedDescriptor, ITimedData feedData){

        for (SubscriptionWindowFeed w : subscriptionWindowFeeds) {
            try {
                var bars = getBarsWithNPeriod(w.getFeedDescriptor().getPeriod(), 1, feedData.getTime());
                w.pushToWindow(bars.get(0));
            } catch (Exception e) {
                System.out.print(String.format("exceptiuones: %s\n", e.toString()));
            }
        }
    }
    protected void feedWindowIndicators(IFeedDescriptor feedDescriptor, ITimedData feedData){

        for (SubscriptionWindowIndicator w : subscriptionWindowIndicators) {
            if (feedDescriptor.equals(w.getFeedDescriptor())) {
                try {
                    w.pushToIndicator((IBar) feedData);
                } catch (Exception e) {
                    System.out.print(String.format("exceptiuones: %s\n", e.toString()));
                }
            }
        }
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
