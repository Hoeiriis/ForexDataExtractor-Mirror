import com.dukascopy.api.*;
import com.dukascopy.api.system.ISystemListener;
import com.dukascopy.api.system.ITesterClient;
import com.dukascopy.api.system.TesterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;
import java.util.concurrent.Future;

public class HistoricalStreamConnector extends DataStreamConnector {

    private static ITesterClient testerClient;
    private static final Logger LOGGER = LoggerFactory.getLogger("DataStreamLog");
    private static String reportsFileLocation = "/home/obliviousmonkey/CoreView/WhatYaWannaKnow/IceRoot_Output_Data/reports/report.html";

    public HistoricalStreamConnector(String userName, String password) throws Exception {
        super(userName, password);
        testerClient.setInitialDeposit(Instrument.EURUSD.getPrimaryJFCurrency(), 50000);
    }

    @Override
    protected void setSystemListener(){
        _client.setSystemListener(new ISystemListener() {
            @Override
            public void onStart(long processId) {
                LOGGER.info("Backtesting started strategy: " + processId);
            }

            @Override
            public void onStop(long processId) {
                LOGGER.info("Backtesting stopped strategy: " + processId);
                File reportFile = new File(reportsFileLocation);
                try {
                    testerClient.createReport(processId, reportFile);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
                if (_client.getStartedStrategies().size() == 0) {
                    System.exit(0);
                }
            }

            @Override
            public void onConnect() {
                LOGGER.info("Connected");
            }

            @Override
            public void onDisconnect() {
                //tester doesn't disconnect
            }
        });
    }

    @Override
    protected void clientInit() throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        testerClient = TesterFactory.getDefaultInstance();
        _client = testerClient;
    }

    @Override
    public void startStrategy(IStrategy strategy) throws Exception
    {
        long strategyId = testerClient.startStrategy(strategy, getLoadingProgressListener());
        LOGGER.info("Strategy started. Assigned id:" + strategyId);
    }

    public void startStrategy(IStrategy strategy, Date fromDate, Date toDate) throws Exception {
        testerClient.setDataInterval(Period.TICK, OfferSide.ASK, ITesterClient.InterpolationMethod.CLOSE_TICK, fromDate.getTime(), toDate.getTime());
        testerClient.setDataInterval(ITesterClient.DataLoadingMethod.DIFFERENT_PRICE_TICKS, fromDate.getTime(), toDate.getTime());
        loadData();
        testerClient.startStrategy(strategy, getLoadingProgressListener());
    }

    private void loadData() throws InterruptedException, java.util.concurrent.ExecutionException {
        //load data
        LOGGER.info("Downloading data");
        Future<?> future = testerClient.downloadData(null);
        //wait for downloading to complete
        future.get();
    }

    private static LoadingProgressListener getLoadingProgressListener() {
        return new LoadingProgressListener() {
            @Override
            public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
                LOGGER.info(information);
            }

            @Override
            public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime) {
            }

            @Override
            public boolean stopJob() {
                return false;
            }
        };
    }

}
