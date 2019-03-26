import com.dukascopy.api.*;
import roots.DataForwarder_Historical;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


public class Main2 {

    public Main2() throws Exception {
    }

    public static void main(String [] args) throws Exception {

        String password = "tEvSP";
        String userName = "DEMO2tEvSP";
        String savePath = "/home/obliviousmonkey/CoreView/WhatYaWannaKnow/IceRoot_Output_Data/test5semireal.csv";

        HistoricalStreamConnector Conn = new HistoricalStreamConnector(userName, password);
        Conn.subscribeToInstrument(Instrument.EURUSD);
        DataForwarder_Historical forwarder = new DataForwarder_Historical(savePath);

        /* Starting */
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+1"));

        Date dateFrom = dateFormat.parse("2018/03/19 08:00:00");
        Date dateTo = dateFormat.parse("2018/03/19 21:00:00");

        Conn.startStrategy(forwarder, dateFrom, dateTo);
    }
}