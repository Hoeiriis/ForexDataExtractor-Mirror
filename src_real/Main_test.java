import comms.other.SocketConnector;

import java.io.IOException;

public class Main_test {

    public static void main(String [] args) throws Exception {
        int portNumber = 9090;
        String ip = "127.0.0.1";

        SocketConnector sconn = new SocketConnector();

        try {
            sconn.startConnection(ip, portNumber);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.print("Sending message");
        sconn.sendMessage("hello hello");
        String resp = sconn.receiveMessage();
        System.out.print(resp);
        sconn.stopConnection();

//        System.out.print("Starting server");
//        socketServer server=new socketServer();
//        try {
//            server.start(6666);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}
