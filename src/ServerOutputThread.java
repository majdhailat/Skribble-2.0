import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ConcurrentModificationException;
import java.util.concurrent.TimeUnit;

/*
sends output to client:
all this class does is get a data package from the server containing all the info the client will need and then
sends that package to the client
 */
class ServerOutputThread extends Thread{
    private Server server;
    private Player player;
    private Socket socket;
    private ObjectOutputStream objectOutputStream;//stream used to send the data package

    public ServerOutputThread(Server server, Player player, Socket socket) throws IOException {
        this.socket = socket;
        this.server = server;
        this.player = player;
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
    }

    public void run(){
        try {
            while (server.isRunning()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {e.printStackTrace();}
                DataPackage dataPackage = server.getDataPackage(player);//getting data package
                try {
                    objectOutputStream.writeUnshared(dataPackage);//sending data package
                    objectOutputStream.flush();
                    //must reset output stream otherwise certain items in the data package might not get update
                    objectOutputStream.reset();
                }catch (ConcurrentModificationException ignore){}
            }
        }
        catch (IOException e) {e.printStackTrace();}
        finally {
            server.playerDisconnected(player);//disconnecting player
            try {
                socket.close();
            } catch (IOException e) {e.printStackTrace();}
        }
    }
}
