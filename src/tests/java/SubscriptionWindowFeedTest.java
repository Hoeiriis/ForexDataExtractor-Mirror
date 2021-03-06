import com.dukascopy.api.IBar;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.api.feed.FeedDescriptor;
import com.dukascopy.api.feed.util.TimePeriodAggregationFeedDescriptor;
import roots.Snapshots.Snapshot;
import roots.Snapshots.SnapshotFeed;
import roots.SubWindows.ISnapshotSubscriber;
import roots.SubWindows.SubscriptionWindowFeed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SubscriptionWindowFeedTest
{
    private SubscriptionWindowFeed testWindow;
    private FeedDescriptor feed;
    private Receiver receiver;
    private int lookback;

    private List<IBar> feedDatas;

    public boolean BarIsEq(IBar truth, IBar test){
        List<Boolean> truths = new ArrayList<>();
        truths.add(truth.getOpen() == test.getOpen());
        truths.add(truth.getClose() == test.getClose());
        truths.add(truth.getLow() == test.getLow());
        truths.add(truth.getHigh() == test.getHigh());
        truths.add(truth.getVolume() == test.getVolume());
        truths.add(truth.getTime() == test.getTime());

        return !truths.contains(false);
    }

    @BeforeAll
    public void globalSetUp(){
        feed = new TimePeriodAggregationFeedDescriptor(Instrument.EURUSD, Period.ONE_MIN, OfferSide.ASK);
        lookback = 30;

        feedDatas = new ArrayList<>();
        for (int i = 0; i < lookback+5 ; i++)
        {
            IBar data = new FeedData(
                    100+i,
                    200+i,
                    300+i,
                    400+i,
                    500+i,
                    600+i
            );

            feedDatas.add(data);
        }
    }

    @BeforeEach
    public void setUp()
    {
        // 1 min Bar, 30 min Lookback, Price/Volume
        testWindow = new SubscriptionWindowFeed(feed, lookback, "test1");
        receiver = new Receiver();
        testWindow.addSubscriber(receiver);
    }

    @Test
    public void SimplePush() throws Exception
    {
        // Arrange
        IBar pushBar = feedDatas.get(0);
        double open = pushBar.getOpen();
        double close = pushBar.getClose();
        double low = pushBar.getLow();
        double high = pushBar.getHigh();
        double vol = pushBar.getVolume();
        long time = pushBar.getTime();

        // Act
        testWindow.pushToWindow(pushBar);
        List<IBar[]> wi = receiver.getReceived();
        IBar outBar = wi.get(0)[0];

        // Assert
        assertEquals(outBar.getOpen(), open);
        assertEquals(outBar.getClose(), close);
        assertEquals(outBar.getLow(), low);
        assertEquals(outBar.getHigh(), high);
        assertEquals(outBar.getVolume(), vol);
        assertEquals(outBar.getTime(), time);
    }

    @Test
    public void MultiplePush() throws Exception
    {
        // Arrange
        List<Double> lows = new ArrayList<>();
        lows.add(feedDatas.get(0).getLow());
        lows.add(feedDatas.get(1).getLow());
        lows.add(feedDatas.get(2).getLow());

        // Act
        testWindow.pushToWindow(feedDatas.get(0));
        testWindow.pushToWindow(feedDatas.get(1));
        testWindow.pushToWindow(feedDatas.get(2));
        List<IBar[]> wi = receiver.getReceived();
        IBar[] latestReceived = wi.get(wi.size()-1);

        int latestLength = latestReceived.length;

        // Assert
        for (int i = 0 ; i < latestLength; i++)
        {
            IBar outBar = latestReceived[i];
            assertEquals(outBar.getLow(), (double)lows.get(latestLength-(i+1)));
            assertTrue(BarIsEq(outBar, feedDatas.get(latestLength-(i+1))));
        }
    }

    @Test
    public void TestLookBack() throws Exception
    {
        // Act
        for (IBar feedData : feedDatas) {
            testWindow.pushToWindow(feedData);
        }

        // Assert
        List<IBar[]> wi = receiver.getReceived();
        IBar[] latestReceived = wi.get(wi.size()-1);
        IBar[] secLatestReceived = wi.get(wi.size()-2);

        int latestLength = latestReceived.length;
        int secLatestLength = secLatestReceived.length;
        assertEquals(latestLength, lookback);
        assertEquals(latestLength, secLatestLength);
    }

    @Test
    public void PushBeyondLookback() throws Exception
    {
        // Act
        for (IBar feedData : feedDatas) {
            testWindow.pushToWindow(feedData);
        }

        // Assert
        List<IBar[]> wi = receiver.getReceived();
        IBar[] latestReceived = wi.get(wi.size()-1);
        List<IBar> latestList = new ArrayList<>(Arrays.asList(latestReceived));


        assertTrue(BarIsEq(feedDatas.get(feedDatas.size()-1), latestReceived[0]));
        assertTrue(!latestList.contains(feedDatas.get(0)));
        assertTrue(BarIsEq(feedDatas.get(5), latestReceived[feedDatas.size()-6]));
    }
}


class Receiver implements ISnapshotSubscriber
{
    public List<IBar[]> received;

    public Receiver()
    {
        received = new ArrayList<>();
    }

    @Override
    public void NewSnapshot(Snapshot newSnapshot) throws Exception
    {
        SnapshotFeed snap = (SnapshotFeed) newSnapshot;
        received.add(snap.getWindow());
    }

    public List<IBar[]> getReceived()
    {
        return received;
    }
}