package roots.SubWindows.Indicators;

import com.dukascopy.api.*;
import com.dukascopy.api.feed.IFeedDescriptor;
import org.apache.commons.lang3.ArrayUtils;
import roots.Snapshots.Snapshot;
import roots.Snapshots.SnapshotIndicator;
import roots.SubWindows.SubscriptionWindowIndicator;

import java.util.ArrayList;
import java.util.List;

public class BollingerBandsWindow extends SubscriptionWindowIndicator<Double[]> {

    private IIndicators.AppliedPrice appliedPrice;
    private int bband_period;
    private IIndicators.MaType maType;
    private double std_dev_from_price;
    private SnapshotIndicator snapshot_base;

    public BollingerBandsWindow(IFeedDescriptor feedDescriptor, int lookBackRange, IIndicators.AppliedPrice appliedPrice, int bband_period, double std_dev_from_price, IIndicators.MaType maType) {
        super(feedDescriptor, lookBackRange);
        this.appliedPrice = appliedPrice;

        this.maType = maType;
        this.bband_period = bband_period;
        this.std_dev_from_price = std_dev_from_price;
        snapshot_base = new SnapshotIndicator(this.id, String.format("BB  LB %d  BB_p %d", lookBackRange, bband_period));
    }

    @Override
    public void pushToIndicator(IBar latestBar) throws JFException {
        this.indicator_window = CalculateIndicator(latestBar);
    }

    @Override
    protected List<Double[]> CalculateIndicator(IBar latestBar) throws JFException {

        double[][] bbands_values = this.indicators.bbands(
                this.feedDescriptor.getInstrument(),
                this.feedDescriptor.getPeriod(),
                OfferSide.ASK,
                appliedPrice,
                bband_period,
                std_dev_from_price,
                std_dev_from_price,
                maType,
                Filter.NO_FILTER,
                this.LookBackRange,
                latestBar.getTime(),
                0
        );

        List<Double[]> returnList = new ArrayList<>();
        for (double[] inner_val : bbands_values) {
            returnList.add(ArrayUtils.toObject(inner_val));
        }
        return returnList;
    }

    @Override
    protected Snapshot SnapshotGenerator()
    {
        snapshot_base.setWindow(this.getWindow());
        return null;
    }

    @Override
    public Double[][] getWindow()
    {
        Double[][] returnWindow = new Double[indicator_window.size()][];
        indicator_window.toArray(returnWindow);
        return returnWindow;
    }
}
