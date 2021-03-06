package roots.SubWindows;

import com.dukascopy.api.Instrument;
import roots.Snapshots.Snapshot;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class SubscriptionWindow<T>
{
    public final UUID id;
    public final int LookBackRange;
    public final Instrument Instrument;
    ArrayDeque<T> Window;
    List<ISnapshotSubscriber> snapshotSubscribers = new ArrayList<>();

    public SubscriptionWindow(Instrument instrument, int lookBackRange)
    {
        id = UUID.randomUUID();
        LookBackRange = lookBackRange;
        Window = new ArrayDeque<T>();
        Instrument = instrument;
    }

    public void pushToWindow(T pushValue) throws Exception {
        Window.push(pushValue);
        while (Window.size() > LookBackRange)
        {
            Window.removeLast();
        }

        notifySubscribers();
    }

    public abstract T[] getWindow();

    protected abstract Snapshot SnapshotGenerator();

    public UUID addSubscriber(ISnapshotSubscriber subscriber){
        snapshotSubscribers.add(subscriber);
        return id;
    }

    public void notifySubscribers() throws Exception {
        Snapshot snapshot = SnapshotGenerator();
        for (ISnapshotSubscriber subscriber: snapshotSubscribers) {
            subscriber.NewSnapshot(snapshot);
        }
    }
}