package roots.Snapshots;

import com.dukascopy.api.IBar;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SnapshotFeed extends Snapshot<IBar>
{
    public SnapshotFeed(UUID subWinID, String description){
        super(subWinID, description);
    }

    @Override
    public Double[] getWindowValues() {
        List<Double> values = new ArrayList<>();

        for (IBar bar : this.Window)
        {
            values.add(bar.getClose());
        }

        return values.toArray(new Double[]{});
    }
}
