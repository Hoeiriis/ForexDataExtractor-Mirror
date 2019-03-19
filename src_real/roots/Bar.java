package roots;

import com.dukascopy.api.IBar;

public class Bar implements IBar {

    public double open;
    public double close;
    public double low;
    public double high;
    public double vol;
    public long time;

    public Bar( double open, double close, double low, double high, double vol, long time)
    {
        this.close = close;
        this.high = high;
        this.low = low;
        this.open = open;
        this.vol = vol;
        this.time = time;
    }

    @Override
    public double getOpen()
    {
        return open;
    }

    @Override
    public double getClose()
    {
        return close;
    }

    @Override
    public double getLow()
    {
        return low;
    }

    @Override
    public double getHigh()
    {
        return high;
    }

    @Override
    public double getVolume()
    {
        return vol;
    }

    @Override
    public long getTime()
    {
        return time;
    }

    public boolean updateHigh(double potentialHigh){
        if(potentialHigh > high){
            high = potentialHigh;
            return true;
        }
        return false;
    }

    public boolean updateLow(double potentialLow){
        if(potentialLow < low){
            low = potentialLow;
            return true;
        }
        return false;
    }

    public void appendVolume(double volumeToAdd){
        vol += volumeToAdd;
    }
}
