import com.dukascopy.api.*;
import roots.DataForwarder_Historical;


public class Main2 {

    public Main2() throws Exception {
    }

    public static void main(String [] args) throws Exception {

        String password = "XQCXf";
        String userName = "DEMO2XQCXf";

        DataStreamConnector Conn = new DataStreamConnector(userName, password, false);
        Conn.subscribeToInstrument(Instrument.EURUSD);
        DataForwarder_Historical forwarder = new DataForwarder_Historical();

        /* Starting */
        Conn.startStrategy(forwarder);
    }
}