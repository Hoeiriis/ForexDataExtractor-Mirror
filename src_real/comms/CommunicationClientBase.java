package comms;

import org.apache.commons.lang3.NotImplementedException;

import java.io.IOException;

public abstract class CommunicationClientBase implements CommunicationClient {

    MsgHandler messageHandler;

    CommunicationClientBase(MsgHandler msgHandler){
        messageHandler = msgHandler;
    }

    @Override
    public abstract void StartConnection(String ip, int port) throws IOException;

    @Override
    public abstract void StopConnection() throws IOException;

    @Override
    public String SendMessage(String msg) throws IOException {
        throw new NotImplementedException("Oh no");
    }

    protected abstract String MessageSender(String msg);
}
