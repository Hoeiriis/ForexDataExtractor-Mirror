package roots;

import com.dukascopy.api.*;
import com.dukascopy.api.feed.IFeedDescriptor;
import com.dukascopy.api.feed.IFeedListener;
import roots.SubWindows.SubscriptionWindowFeed;
import com.dukascopy.api.IIndicators;
import roots.SubWindows.SubscriptionWindowIndicator;

import java.util.List;

public class DataForwarder implements IStrategy, IFeedListener {

    public List<SubscriptionWindowFeed> subscriptionWindowFeeds;
    public List<SubscriptionWindowIndicator> subscriptionWindowIndicators;
    private IHistory history;


    public DataForwarder(List<SubscriptionWindowFeed> subscriptionWindowFeeds, List<SubscriptionWindowIndicator> subscriptionWindowIndicators)
    {
        this.subscriptionWindowFeeds = subscriptionWindowFeeds;
        this.subscriptionWindowIndicators = subscriptionWindowIndicators;
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
        }

        IIndicators indicators = context.getIndicators();

        for (SubscriptionWindowIndicator subWinInd : subscriptionWindowIndicators){
            context.subscribeToFeed(subWinInd.getFeedDescriptor(), this);
            subWinInd.setIndicators(indicators);
        }
    }

    @Override
    public void onFeedData(IFeedDescriptor feedDescriptor, ITimedData feedData)
    {
        feedWindowFeeds(feedDescriptor, feedData);
        feedWindowIndicators(feedDescriptor, feedData);
    }

    private void feedWindowFeeds(IFeedDescriptor feedDescriptor, ITimedData feedData){

        for (SubscriptionWindowFeed w : subscriptionWindowFeeds) {
            if (feedDescriptor.equals(w.FeedDescriptor)) {
                try {
                    w.pushToWindow((IBar) feedData);
                } catch (Exception e) {
                    System.out.print(String.format("exceptiuones: %s\n", e.toString()));
                }
            }
        }
    }

    private void feedWindowIndicators(IFeedDescriptor feedDescriptor, ITimedData feedData){

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
