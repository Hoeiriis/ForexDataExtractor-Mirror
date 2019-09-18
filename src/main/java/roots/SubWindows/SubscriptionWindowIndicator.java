package roots.SubWindows;

import com.dukascopy.api.feed.IFeedDescriptor;
import com.dukascopy.api.*;
import roots.Snapshots.Snapshot;

import java.io.NotActiveException;
import java.util.List;

public abstract class SubscriptionWindowIndicator<T> extends SubscriptionWindow<T> implements ISnapshotSubscriber{

    protected List<T> indicator_window;
    protected IIndicators indicators;
    protected IFeedDescriptor feedDescriptor;

    public SubscriptionWindowIndicator(IFeedDescriptor feedDescriptor, int lookBackRange) {
        super(feedDescriptor.getInstrument(), lookBackRange);
        this.feedDescriptor = feedDescriptor;
    }

    public void setIndicators(IIndicators indicators) {
        this.indicators = indicators;
    }

    public IFeedDescriptor getFeedDescriptor() {return  feedDescriptor; }

    @Override
    public void NewSnapshot(Snapshot newSnapshot) throws Exception {
        if(indicators == null){
            throw new NotActiveException("indicators has not been set");
        }
        IBar[] current_window = (IBar[]) newSnapshot.getWindow();
        this.indicator_window = CalculateIndicator(current_window[0]);
        notifySubscribers();
    }

    @Override
    public void pushToWindow(T pushValue) throws NotActiveException {
        throw new NotActiveException("This method is not active");
    }

    public abstract void pushToIndicator(IBar latestBar) throws Exception;

    protected abstract List<T> CalculateIndicator(IBar latestBar) throws JFException;
}
