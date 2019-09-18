package roots.SubWindows;

import roots.Snapshots.Snapshot;

public interface ISnapshotSubscriber {

    void NewSnapshot(Snapshot newSnapshot) throws Exception;
}
