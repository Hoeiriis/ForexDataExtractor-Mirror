package comms;

import java.io.IOException;

public interface CommunicationClient {

    void StartConnection(String ip, int port) throws IOException;

    void StopConnection() throws IOException;

    String SendMessage(String msg) throws IOException;

}
