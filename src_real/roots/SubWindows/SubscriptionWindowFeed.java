package roots.SubWindows;

import com.dukascopy.api.IBar;
import com.dukascopy.api.feed.IFeedDescriptor;
import roots.Snapshots.Snapshot;
import roots.Snapshots.SnapshotFeed;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;

public class SubscriptionWindowFeed extends SubscriptionWindow<IBar>
{
    public final IFeedDescriptor FeedDescriptor;
    private SnapshotFeed snapshot_base;

    public SubscriptionWindowFeed(IFeedDescriptor feedDescriptor, int lookBackRange, String name)
    {
        super(feedDescriptor.getInstrument(), lookBackRange);
        FeedDescriptor = feedDescriptor;
        snapshot_base = new SnapshotFeed(this.id, name);
    }

    public IFeedDescriptor getFeedDescriptor(){
        return FeedDescriptor;
    }

    public void setWindow(IBar[] bars){
        List<IBar> iBarList = Arrays.asList(bars);
        this.Window = new ArrayDeque<>(iBarList);
    }

    @Override
    public void pushToWindow(IBar pushValue) throws Exception {
        Window.push(pushValue);
        while (Window.size() > LookBackRange)
        {
            Window.removeLast();
        }

        notifySubscribers();
    }

    @Override
    public IBar[] getWindow()
    {
        IBar[] returnBars = new IBar[Window.size()];
        Window.toArray(returnBars);
        return returnBars;
    }

    @Override
    protected Snapshot SnapshotGenerator()
    {
        snapshot_base.setWindow(this.getWindow());
        return snapshot_base;
    }

}