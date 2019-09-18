package roots.Snapshots;

import roots.ThingsThatShouldBeEasyInJavaButAreNot;

import java.util.UUID;

public class SnapshotIndicator extends Snapshot<Double[]>
{
    public final IndicatorType indicatorType;

    public SnapshotIndicator(UUID subWinID, String description, IndicatorType iType){
        super(subWinID, description);
        this.indicatorType=iType;
    }

    @Override
    public Double[] getWindowValues() {
        return ThingsThatShouldBeEasyInJavaButAreNot.flatten2DDoubleArray(this.Window);
    }
}

