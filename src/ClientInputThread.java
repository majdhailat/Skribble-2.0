import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class ClientInputThread extends Thread {
    private ObjectInputStream objectInputStream;//this stream is used to read the data package object
    //the messages that have been read from the messages only for me list in the player
    private Client client;

    public ClientInputThread(Socket socket, Client client) throws IOException {
        objectInputStream = new ObjectInputStream(socket.getInputStream());
        this.client = client;
    }

    public void run() {
        try {
            while (client.isRunning()) {
                DataPackage dataPackage = (DataPackage) objectInputStream.readUnshared();
                client.receivedDataPackage(dataPackage);
            }
        }catch (IOException | ClassNotFoundException e) {e.printStackTrace();}
    }
}