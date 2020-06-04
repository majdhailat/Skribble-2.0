//imports
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/*
Listens for new clients and starts the server I/O threads when a client joins
 */
class ListenForClients extends Thread{
    private int numberOfClients = 0;
    private Server server;
    public ListenForClients(Server server) {
        this.server = server;
    }

    public void run( ){
        int portNumber = 4445;//PORT
        boolean listening = true;
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            while (listening) {
                Socket socket = serverSocket.accept();//waiting for clients to connect
                Player player = new Player();//creating the clients player object
                //creating input and output threads used to communicate with the client
                new ServerOutputThread(server, player, socket).start();
                try {
                    TimeUnit.MILLISECONDS.sleep(5000);
                } catch (InterruptedException e) {e.printStackTrace();}
                new ServerInputThread(server, player, socket).start();
                numberOfClients ++;
                if (numberOfClients >= 6){
                    listening = false;
                    System.out.println("No more users may join");
                }
            }
        } catch (IOException e) {
            System.err.println("Port error");
            System.exit(-1);
        }
    }
}