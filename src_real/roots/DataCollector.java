package roots;

import com.dukascopy.api.IBar;
import roots.Snapshots.Snapshot;
import roots.Snapshots.SnapshotFeed;
import roots.Snapshots.SnapshotIndicator;
import roots.Snapshots.SnapshotTarget;
import roots.SubWindows.ISnapshotSubscriber;
import roots.SubWindows.SubscriptionWindow;

import java.util.*;

public class DataCollector implements ISnapshotSubscriber
{
    private Map<UUID, Double[]> featureCollection;
    private Map<UUID, String> featureDescription;
    private Map<UUID, int[]> featureIndices;
    public ArrayList<Double> features;
    public boolean pushForward;

    public DataCollector(boolean startPushing)
    {
        featureDescription = new HashMap<>();
        featureCollection = new HashMap<>();
        featureIndices = new HashMap<>();
        features = new ArrayList<>();

        pushForward = startPushing;
    }


    @Override
    public void NewSnapshot(Snapshot newSnapshot) throws Exception
    {
        UpdateSnapShot(newSnapshot);

        if(pushForward) {
            DisplayFeatures();
        }
    }

    public void autoSubscribe(SubscriptionWindow[] subscriptionWindows)
    {
        for (SubscriptionWindow window: subscriptionWindows)
        {
            window.addSubscriber(this);
        }
    }

    private void UpdateSnapShot(Snapshot snapshot) throws Exception
    {
        var values = snapshot.getWindowValues();
        if(featureIndices.containsKey(snapshot.id))
        {
            UpdateFeatures(values, featureIndices.get(snapshot.id));
            featureCollection.put(snapshot.id, values);
        }
        else
        {
            int[] indices = GetNewIndicesAndUpdateFeatures(values.length, values);
            featureIndices.put(snapshot.id, indices);
            featureCollection.put(snapshot.id, values);
            featureDescription.put(snapshot.id, snapshot.description);
        }
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

    private int[] GetNewIndicesAndUpdateFeatures(int featureLength, Double[] newValues){
        int[] indices = new int[2];
        indices[0] = features.size();
        indices[1] = features.size() + featureLength;
        features.ensureCapacity(features.size() + featureLength);
        features.addAll(Arrays.asList(newValues));

        return indices;
    }

    public void DisplayFeatures(){
        System.out.print("\nNmb: "+features.size()+"\nFeatures:"+features.toString());
    }

    public Map<UUID, Double[]> getFeatureCollection() {
        return featureCollection;
    }

    public Map<UUID, String> getFeatureDescription() {
        return featureDescription;
    }
}
