package comms;

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
    public void SendMessage(String msg) throws IOException {
    }

    protected abstract String MessageSender(String msg);
}
