import com.dukascopy.api.IStrategy;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.system.ClientFactory;
import com.dukascopy.api.system.IClient;
import com.dukascopy.api.system.ISystemListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class DataStreamConnector
{
    private String _jnlpUrl = "http://platform.dukascopy.com/demo/jforex.jnlp";
    private String _userName;
    private String _password;

    protected IClient _client;
    private final Logger LOGGER = LoggerFactory.getLogger("DataStreamLog");
    private Set<Instrument> _instruments = new HashSet<>();

    private int lightReconnects = 3;

    public DataStreamConnector(String userName, String password, boolean live_connection) throws Exception
    {
        _userName = userName;
        _password = password;
        clientInit();

        if (live_connection){
            _jnlpUrl = "live version";
            LOGGER.info("Connection set to live");
        }

        setSystemListener();
        tryToConnect();
        subscribeToInstrument(Instrument.EURUSD);

        LOGGER.info("StreamConnector ready and running");
    }


    public DataStreamConnector(String userName, String password) throws Exception
    {
        this(userName, password, false);
    }

    public void subscribeToInstrument(Instrument subInstrument) {
        _instruments.add(subInstrument);
        LOGGER.info(String.format("Subscribing to instrument: %s", subInstrument.getName()));
        _client.setSubscribedInstruments(_instruments);
    }

    public void startStrategy(IStrategy strategy) throws Exception
    {
        long strategyId = _client.startStrategy(strategy);
        LOGGER.info("Strategy started. Assigned id:" + strategyId);
    }

    protected void clientInit() throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        _client = ClientFactory.getDefaultInstance();
    }

    protected void setSystemListener() {
        //set the listener that will receive system events
        _client.setSystemListener(new ISystemListener() {

            @Override
            public void onStart(long processId)
            {
                LOGGER.info("Strategy started: " + processId);
            }

            @Override
            public void onStop(long processId)
            {
                LOGGER.info("Strategy stopped: " + processId);
                if (_client.getStartedStrategies().size() == 0) {
                    System.exit(0);
                }
            }

            @Override
            public void onConnect() {
                LOGGER.info("Connected");
                lightReconnects = 3;
            }

            @Override
            public void onDisconnect() {
                tryToReconnect();
            }
        });
    }

    private void tryToConnect() throws Exception {
        LOGGER.info("Connecting to Dukascopy");
        //connect to the server using jnlp, user name and password
        _client.connect(_jnlpUrl, _userName, _password);

        //wait for it to connect
        int i = 10; //wait max ten seconds
        while (i > 0 && !_client.isConnected()) {
            Thread.sleep(1000);
            i--;
        }
        if (!_client.isConnected()) {
            LOGGER.error("Failed to connect to Dukascopy servers");
            System.exit(1);
        }
    }

    private void tryToReconnect() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (lightReconnects > 0) {
                    _client.reconnect();
                    --lightReconnects;
                } else {
                    do {
                        try {
                            Thread.sleep(60 * 1000);
                        } catch (InterruptedException ignored) {
                        }
                        try {
                            if(_client.isConnected()) {
                                break;
                            }
                            _client.connect(_jnlpUrl, _userName, _password);

                        } catch (Exception e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                    } while(!_client.isConnected());
                }
            }
        };
        new Thread(runnable).start();
    }
}
