import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketException;

/*
Gets input from client:

If the client is an artist this thread gets the drawing components (the canvas instructions) from the client
and updates it to the server.

If the client is not an artist this thread simply reads messages from the client, these messages are either
text messages typed by the client in the chat box or commands form the client code to do things like start
the game or set the players user name
 */
class InputThread extends Thread {
    private Server server;
    private Player player;
    private Socket socket;
    //This stream is used for both the drawing components array and the string text messages
    private ObjectInputStream in;
    private boolean gotUserName = false;//if the player has input their user name

    public InputThread(Server server, Player player, Socket socket) throws IOException {
        this.socket = socket;
        this.server = server;
        this.player = player;
        in = new ObjectInputStream(socket.getInputStream());
    }

    public void run() {
        try {
            while (server.isRunning()) {
                //ARTIST MODE
                if (Player.getArtist() == player){
                    try {
                        //WAITING for the client to send a drawing component array, then reading it from the client
                        DrawingComponent[] shapesArray = (DrawingComponent[]) in.readObject();
                        server.setDrawingComponents(shapesArray);//setting the drawing components in the server
                    }catch(ClassCastException ignored){}
                    //this exception WILL happen when the user goes from artist to non artist because the in stream
                    //will be waiting for a drawing component array and then will receive a string and try to cast
                    //the string to a drawing component array. The string that causes this exception will
                    //not reach the server
                } else {
                    //NON ARTIST MODE
                    String inputLine;
                    try {
                        //WAITING for the client to send a string then reading that string
                        if ((inputLine = (String) in.readObject()) != null) {
                            if (!gotUserName) {//checking if user name has not been obtained
                                System.out.println("got name from client");
                                player.setName(inputLine);//setting user name
                                server.playerConnected(player);//alerting server of new player
                                gotUserName = true;
                                //checking if the user triggered the game to start
                            } else if (server.getGameStatus().equals(DataPackage.WAITINGTOSTART )&& inputLine.equals("/START") && server.getPlayers().get(0) == player) {
                                server.newGame();//starting new game
                            } else {//just a message - not a command
                                server.newMessage(inputLine, player);
                            }
                        }
                    }catch (SocketException | ClassCastException ignored){}
                    //this exception is here for the same exact reason as the class cast exception above except
                    //now a drawing component array is trying to be casted to a string
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