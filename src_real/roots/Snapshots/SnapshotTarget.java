package roots.Snapshots;

import java.util.UUID;

public class SnapshotTarget extends Snapshot<Double> {

    public SnapshotTarget(UUID subWinID, String description) {
        super(subWinID, description);
    }

    @Override
    public Double[] getWindowValues() {
        return getWindow();
    }
}
