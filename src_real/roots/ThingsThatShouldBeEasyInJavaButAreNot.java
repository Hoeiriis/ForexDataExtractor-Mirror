package roots;

import org.apache.commons.lang3.ArrayUtils;

/*
    Or probably I'm just stupid
*/
public class ThingsThatShouldBeEasyInJavaButAreNot
{
    public static Double[] flatten2DDoubleArray(Double[][] arrayToFlatten){
        Double[] outArray = new Double[0];

        for (Double[] subarray : arrayToFlatten)
        {
            outArray = ArrayUtils.addAll(outArray, subarray);
        }

        return outArray;
    }
}
