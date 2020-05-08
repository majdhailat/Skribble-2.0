import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

class ClientInputThread extends Thread {
    private ObjectInputStream objectInputStream;//stream used to read the data package object
    private Client client;

    public ClientInputThread(Socket socket, Client client) throws IOException {
        objectInputStream = new ObjectInputStream(socket.getInputStream());
        this.client = client;
    }

    public void run() {
        try {
            while (client.isRunning()) {
                DataPackage dataPackage = (DataPackage) objectInputStream.readUnshared();
                client.updateDataPackage(dataPackage);
            }
        }catch (IOException | ClassNotFoundException ignored) {}
    }
}
