package roots.SubWindows.Indicators;

import com.dukascopy.api.*;
import com.dukascopy.api.feed.IFeedDescriptor;
import org.apache.commons.lang3.ArrayUtils;
import roots.Snapshots.IndicatorType;
import roots.Snapshots.Snapshot;
import roots.Snapshots.SnapshotIndicator;
import roots.SubWindows.SubscriptionWindowIndicator;

import java.util.Arrays;
import java.util.List;

public class RSIWindow extends SubscriptionWindowIndicator<Double> {

    private IIndicators.AppliedPrice appliedPrice;
    private int rsi_period;
    private SnapshotIndicator snapshot_base;

    public RSIWindow(IFeedDescriptor feedDescriptor, int lookBackRange, IIndicators.AppliedPrice appliedPrice, int rsi_period, String name) {
        super(feedDescriptor, lookBackRange);
        this.appliedPrice = appliedPrice;
        this.rsi_period = rsi_period;

        snapshot_base = new SnapshotIndicator(this.id, name, IndicatorType.RSI);
    }

    @Override
    public void pushToIndicator(IBar latestBar) throws Exception {
        this.indicator_window = CalculateIndicator(latestBar);
        notifySubscribers();
    }

    @Override
    protected List<Double> CalculateIndicator(IBar latestBar) throws JFException {

        double[] rsi_values = this.indicators.rsi(
                this.feedDescriptor.getInstrument(),
                this.feedDescriptor.getPeriod(),
                OfferSide.ASK,
                appliedPrice,
                rsi_period,
                Filter.NO_FILTER,
                this.LookBackRange,
                latestBar.getTime(),
                0
        );
        Double[] doubleArray = ArrayUtils.toObject(rsi_values);
        return Arrays.asList(doubleArray);
    }

    @Override
    public Double[] getWindow()
    {
        Double[] returnWindow = new Double[indicator_window.size()];
        indicator_window.toArray(returnWindow);
        return returnWindow;
    }

    @Override
    protected Snapshot SnapshotGenerator()
    {
        snapshot_base.setWindow(new Double[][] {this.getWindow()});
        return snapshot_base;
    }
}