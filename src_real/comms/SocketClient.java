package comms;
import java.net.*;
import java.io.*;
import java.util.Scanner;

public class SocketClient implements CommunicationClient {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private MsgHandler messageHandler;

    SocketClient(MsgHandler msgHandler){
        messageHandler = msgHandler;
    }

    public void StartConnection(String ip, int port) throws IOException {
        clientSocket = new Socket(ip, port);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public String SendMessage(String msg) throws IOException {
        out.println(msg);
        out.flush();
        return in.readLine();
    }

    public void StopConnection() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
    }

    protected void HandleResponses(){

        while(in.hasNextLine()){
            var line = in.nextLine();
            if (line.startsWith("MSG")){
                messageHandler.HandleMessage(line);
            }
        }
    }
}

class SocketCommunicator extends Thread {
    Socket serverSocket;

    SocketCommunicator(Socket serverSocket){
        this.serverSocket = serverSocket;
    }

    public void run(){

    }
}