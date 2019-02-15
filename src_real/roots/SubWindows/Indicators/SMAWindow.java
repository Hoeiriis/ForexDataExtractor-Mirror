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

public class SMAWindow extends SubscriptionWindowIndicator<Double> {

    private IIndicators.AppliedPrice appliedPrice;
    private int sma_period;
    private SnapshotIndicator snapshot_base;

    public SMAWindow(IFeedDescriptor feedDescriptor, int lookBackRange, IIndicators.AppliedPrice appliedPrice, int sma_period) {
        super(feedDescriptor, lookBackRange);
        this.appliedPrice = appliedPrice;
        this.sma_period = sma_period;

        snapshot_base = new SnapshotIndicator(this.id, String.format("SMA  LB %d  sma_p %d", lookBackRange, sma_period), IndicatorType.SMA);
    }

    @Override
    public void pushToIndicator(IBar latestBar) throws JFException {
        this.indicator_window = CalculateIndicator(latestBar);
    }

    @Override
    protected List<Double> CalculateIndicator(IBar latestBar) throws JFException {

        double[] sma_values = this.indicators.sma(
                this.feedDescriptor.getInstrument(),
                this.feedDescriptor.getPeriod(),
                OfferSide.ASK,
                appliedPrice,
                sma_period,
                Filter.NO_FILTER,
                this.LookBackRange,
                latestBar.getTime(),
                0
        );
        Double[] doubleArray = ArrayUtils.toObject(sma_values);
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
        return null;
    }
}
