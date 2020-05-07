import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ConcurrentModificationException;
import java.util.concurrent.TimeUnit;

/*
Gets input from client:

If the client is an artist this thread gets the drawing components (the canvas instructions) from the client
and updates it to the server.

If the client is not an artist this thread simply reads messages from the client, these messages are either
text messages typed by the client in the chat box or commands form the client code to do things like start
the game or set the players user name
 */
class ServerInputThread extends Thread {
    private Server server;
    private Player player;
    private Socket socket;
    private ObjectInputStream in;//stream used to receive drawing components and the text messages

    public ServerInputThread(Server server, Player player, Socket socket) throws IOException {
        this.socket = socket;
        this.server = server;
        this.player = player;
        in = new ObjectInputStream(socket.getInputStream());
    }

    public void run() {
        try {
            while (server.isRunning()) {
                //ARTIST MODE
                if (player.isArtist()){
                    try {
                        //WAITING for the client to send a drawing component array, then reading it from the client
                        DrawingComponent[] shapesArray = (DrawingComponent[]) in.readObject();
                        server.setDrawingComponents(shapesArray);//setting the drawing components in the server
                    }catch(ClassCastException ignored){}
                } else {
                    //NON ARTIST MODE
                    String inputLine;
                    try {
                        //WAITING for the client to send a string then reading that string
                        if ((inputLine = (String) in.readObject()) != null) {
                            server.newMessage(inputLine, player);
                        }
                    }catch (SocketException | ClassCastException ignored){}
                }
            }
        } catch(IOException | ClassNotFoundException e){e.printStackTrace();}
        finally{
            server.playerDisconnected(player);//disconnecting player
            try {
                socket.close();//closing socket
            } catch (IOException e) {e.printStackTrace();}
        }
    }
}

