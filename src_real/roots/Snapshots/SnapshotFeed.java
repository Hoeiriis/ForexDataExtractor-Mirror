package roots.Snapshots;

import com.dukascopy.api.IBar;

import java.util.UUID;

public class SnapshotFeed extends Snapshot<IBar>
{
    public SnapshotFeed(UUID subWinID, String description){
        super(subWinID, description);
    }
}
