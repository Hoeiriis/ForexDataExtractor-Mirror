import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import roots.DataCollector;
import roots.Snapshots.IndicatorType;
import roots.Snapshots.SnapshotIndicator;

import java.util.Map;
import java.util.UUID;

public class DataCollectorTest
{
    private DataCollector theCollector;
    private UUID[] dataPusher;
    private int pushNumber;

    @BeforeClass
    public void globalSetUp()
    {
        UUID id0 = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        UUID id4 = UUID.randomUUID();
        dataPusher = new UUID[] {id0, id1, id2, id3, id4};
    }

    @BeforeMethod
    public void setUp()
    {
        theCollector = new DataCollector(true);
        pushNumber = 0;
    }

    @Test
    public void TestSingleSnapshotUUID() throws Exception {

        // Arrange
        double expected1 = 1; // First repeat
        double expected2 = 1; // First amount

        // Act
        pushData(1, 1);

        // Assert
        Assert.assertEquals(theCollector.features.get(0), expected1);
        Assert.assertEquals(theCollector.features.get(1), expected2);
    }

    @Test
    public void TestMultipleSnapshotUUID() throws Exception {

        // Arrange
        double expected1 = 1; // First repeat
        double expected2 = 1; // First amount
        double expected3 = 1; // First repeat
        double expected4 = 2; // Second amount
        double expected5 = 1; // First repeat
        double expected6 = 3; // Third amount

        // Act
        pushData(3, 1);

        // Assert
        Assert.assertEquals(theCollector.features.get(0), expected1);
        Assert.assertEquals(theCollector.features.get(1), expected2);
        Assert.assertEquals(theCollector.features.get(2), expected3);
        Assert.assertEquals(theCollector.features.get(3), expected4);
        Assert.assertEquals(theCollector.features.get(4), expected5);
        Assert.assertEquals(theCollector.features.get(5), expected6);
    }

    @Test
    public void TestSingleFeatureUpdate() throws Exception {

        // Arrange
        double expected1 = 2; // Second repeat
        double expected2 = 1; // First amount

        // Act
        pushData(1, 2);

        // Assert
        Assert.assertEquals(theCollector.features.get(0), expected1);
        Assert.assertEquals(theCollector.features.get(1), expected2);
    }

    @Test
    public void TestMultipleFeaturesUpdates() throws Exception{

        // Arrange
        double expected1 = 4;
        double expected2 = 1;
        double expected3 = 4;
        double expected4 = 2;
        double expected5 = 4;
        double expected6 = 3;

        // Act
        pushData(3, 4);

        // Assert
        Assert.assertEquals(theCollector.features.get(0), expected1);
        Assert.assertEquals(theCollector.features.get(1), expected2);
        Assert.assertEquals(theCollector.features.get(2), expected3);
        Assert.assertEquals(theCollector.features.get(3), expected4);
        Assert.assertEquals(theCollector.features.get(4), expected5);
        Assert.assertEquals(theCollector.features.get(5), expected6);
    }

    public void pushData(int amount, int repeats) throws Exception {
        amount = amount > 5 ? 5 : amount;

        for (int i = 0; i < repeats; i++)
        {
            for (int j = 0; j < amount; j++)
            {
                SnapshotIndicator pushdata = new SnapshotIndicator(dataPusher[j], "lol", IndicatorType.SMA);
                pushdata.setWindow(new Double[][] {{(double) i+1, (double) j+1}});
                theCollector.NewSnapshot(pushdata);
            }
        }
    }
}