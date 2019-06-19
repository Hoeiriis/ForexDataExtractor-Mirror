package comms.other;
import java.net.*;
import java.io.*;

public class SocketConnector {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public void startConnection(String ip, int port) throws IOException {
        clientSocket = new Socket(ip, port);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public void sendMessage(String msg) throws IOException {
        out.println(msg);

        String res = in.readLine();

//        StringBuilder builder = new StringBuilder();
//        for(int c = 0; (c=in.read()) != -1;){
//            builder.append(String.valueOf((char) c));
//        }
//
//        System.out.print(builder.toString());
        System.out.print(res);
    }

    public String receiveMessage(){
        boolean looper = true;
        String msg = null;

        return msg;
    }

    public void stopConnection() throws IOException {
        out.println("Closing Connection");
        in.close();
        out.close();
        clientSocket.close();
    }
}
