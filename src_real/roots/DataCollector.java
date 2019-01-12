package roots;

import com.dukascopy.api.IBar;
import roots.Snapshots.Snapshot;
import roots.Snapshots.SnapshotFeed;
import roots.Snapshots.SnapshotIndicator;
import roots.SubWindows.ISnapshotSubscriber;

import java.util.*;

public class DataCollector implements ISnapshotSubscriber
{
    private Map<UUID, Double[]> windowCollection;
    private Map<UUID, int[]> featureIndices;
    public List<Double> features;

    public DataCollector()
    {
        windowCollection = new HashMap<>();
        featureIndices = new HashMap<>();
        features = new ArrayList<>();
    }


    @Override
    public void NewSnapshot(Snapshot newSnapshot) throws Exception
    {
        if (newSnapshot instanceof SnapshotFeed){
            IfSnapShotFeed((SnapshotFeed) newSnapshot);
        }
    }

    private void IfSnapShotFeed(SnapshotFeed snapshot) throws Exception
    {
        IBar[] bars = snapshot.getWindow();
        List<Double> values = new ArrayList<>();

        for (IBar bar : bars)
        {
            values.add(bar.getClose());
        }

        if(featureIndices.containsKey(snapshot.id))
        {
            UpdateFeatures(values.toArray(new Double[0]), featureIndices.get(snapshot.id));
        }
        else
        {
            int[] indices = GetNewIndices(values.toArray(new Double[0]));
            featureIndices.put(snapshot.id, indices);
            UpdateFeatures(values.toArray(new Double[0]), indices);
        }
    }

    private void IfSnapshotIndicator(SnapshotIndicator snapshot){

    }

    private void UpdateFeatures(Double[] newValues, int[] featureIndices) throws Exception{

        if(newValues.length != (featureIndices[1] - featureIndices[0])){
            throw new Exception("newValues is not equivalent in length to its assigned place in features");
        }

        int initialIndices = featureIndices[0];

        for(int i = 0; i < newValues.length; i += 1 ){
            features.set(initialIndices+i, newValues[i]);
        }
    }

    private int[] GetNewIndices(Double[] newValues){
        int[] indices = new int[2];
        indices[0] = features.size();
        indices[1] = features.size() + newValues.length;

        return indices;
    }
}
