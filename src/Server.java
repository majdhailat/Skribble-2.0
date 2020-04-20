//Imports
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

//Stores all the global information about the game and starts the listening thread
public class Server {
    private boolean running = true;
    private String gameState = DataPackage.WAITINGFORPLAYERS;
    private DrawingComponent[] drawingComponents = null;
    private String message;//the text message
    private Player artist;
    private ArrayList<Player> messageReaders = new ArrayList<Player>();//the players who read the text message
    private ArrayList<Player> players = new ArrayList<Player>();
    private ArrayList<Player> winners = new ArrayList<Player>();
    private String currentMagicWord; //should be a read from a txt file
    private ArrayList<String> magicWords = new ArrayList<String>();

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        new ListenForClients(server).start();
        server.loadMagicWords("words");
    }

    public boolean isRunning(){return running;}

    public synchronized void newPlayer(Player player){
        players.add(player);
    }

    public synchronized void newMessage(String msg, Player player){

        messageReaders.clear();
        if (msg.contains("~")) {//this means that the client is sending a message and not the user
            message = msg;
        } else {
            /*
            if(msg.equals(currentMagicWord)){
                winners.add(player);
                message = player.getName() + " has guessed correctly!";
                if(winners.size() == players.size()){
                    System.out.println("new round");

                    //newRound();
                }
            }

             */
            //else{
            message = (player.getName()) + ": " + msg;
            //}
        }
        messageReaders.add(player);

    }

    public synchronized void updateShapes(DrawingComponent[] shapesArray) {
        this.drawingComponents = shapesArray;
    }

    public synchronized String getMessage(Player player) {
        if (messageReaders.contains(player)){
            return null;
        }else{
            messageReaders.add(player);
            return message;
        }
    }

    public synchronized void playerDisconnected(Player player){
        newMessage("#FF0000~"+player.getName()+" has left the game", player);
        players.remove(player);
    }

    //groups all of the global information into 1 object for distribution to all clients
    public synchronized DataPackage getDataPackage(Player player){
        try {
            TimeUnit.MILLISECONDS.sleep(1);
        } catch (InterruptedException e) {e.printStackTrace();}
        return new DataPackage(getMessage(player), players, winners, player, artist, drawingComponents, gameState);
    }

    public void newRound(){
        //artist = players.get(randint(0, players.size()-1)); //or we could go through the list
        artist = players.get(1);
        winners = new ArrayList<Player>();
        currentMagicWord = magicWords.get(randint(0,magicWords.size()-1));
    }

    public void loadMagicWords(String filename) throws IOException{
        Scanner inFile = new Scanner(new BufferedReader(new FileReader(filename)));
        int numWords = inFile.nextInt();
        inFile.nextLine();
        for(int i = 0; i < numWords; i++){
            magicWords.add(inFile.nextLine());
        }
        inFile.close();
    }

    public void setGameState(String state){
        if (state.equals(DataPackage.GAMESTARTING) || state.equals(DataPackage.ROUNDINPROGRESS) || state.equals(DataPackage.WAITINGFORPLAYERS)){
            gameState = state;
        }
        if (gameState.equals(DataPackage.GAMESTARTING)){
            newRound();
        }
    }

    public String getGameState(){
        return gameState;
    }



    public boolean isArtist(Player player){
        return (player == artist);
    }

    public static int randint(int low, int high){
        //this method returns a random int between the low and high args inclusive
        return (int)(Math.random()*(high-low+1)+low);
    }

    public List<Player> getPlayers() {
        return players;
    }
}

// ------------ Listening Thread ------------------------------------------
//Listens for new clients and creates the I/O threads when a client joins
class ListenForClients extends Thread{
    private Server server;
    public ListenForClients(Server server) {
        this.server = server;
    }

    public void run( ){
        int portNumber = 4445;
        boolean listening = true;
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            while (listening) {
                Socket socket = serverSocket.accept();
                Player player = new Player("");
                new ServerChatInputThread(server, player, socket).start();
                new ServerOutputThread(server, player, socket).start();
            }
        } catch (IOException e) {
            System.err.println("Port error");
            System.exit(-1);
        }
    }
}

// ------------ I/O TO Client Threads ------------------------------------------
//Reads messages from the client and sends them to the server
class ServerOutputThread extends Thread {
    private Server server;
    private Player player;
    private Socket socket;
    private ObjectInputStream in;
    private boolean gotUserName = false;

    public ServerOutputThread(Server server, Player player, Socket socket) throws IOException {
        this.socket = socket;
        this.server = server;
        this.player = player;
        in = new ObjectInputStream(socket.getInputStream());
    }

    public void run() {
        try {
            while (server.isRunning()) {
                if (server.isArtist(player)) {
                    try {
                        DrawingComponent[] shapesArray = (DrawingComponent[]) in.readObject();
                        if (shapesArray.length > 0) {
                            server.updateShapes(shapesArray);
                        }
                    } catch (ClassNotFoundException | OptionalDataException e) {
                        e.printStackTrace();
                    }
                } else {
                    String inputLine;
                    try {
                        if ((inputLine = (String) in.readObject()) != null) {

                            if (!gotUserName) {
                                player.setName(inputLine);
                                server.newMessage(("#FF0000~" + inputLine + " has joined the game"), player);
                                server.newPlayer(player);
                                gotUserName = true;
                            } else if (server.getGameState().equals(DataPackage.WAITINGFORPLAYERS) && inputLine.equals("START") && server.getPlayers().get(0) == player) {
                                server.setGameState(DataPackage.GAMESTARTING);
                            } else {
                                server.newMessage(inputLine, player);
                            }
                        }
                    }catch (ClassCastException ignored){

                    }
                }
            }

        } catch(IOException | ClassNotFoundException e){e.printStackTrace();}

        finally{
            server.playerDisconnected(player);
            try {
                socket.close();
            } catch (IOException e) {e.printStackTrace();}
        }
    }
}

//Gets the data package from the server and sends it to the client
class ServerChatInputThread extends Thread{
    private Server server;
    private Player player;
    private Socket socket;
    private ObjectOutputStream objectOutputStream;
    public ServerChatInputThread(Server server, Player player, Socket socket) throws IOException {
        this.socket = socket;
        this.server = server;
        this.player = player;
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
    }

    public void run(){
        try {
            while (server.isRunning()) {
                DataPackage dataPackage = server.getDataPackage(player);
                objectOutputStream.writeUnshared(dataPackage);
                objectOutputStream.flush();
                objectOutputStream.reset();
            }
        }
        catch (IOException e) {e.printStackTrace();}
        finally {
            try {
                socket.close();
            } catch (IOException e) {e.printStackTrace();}
        }
    }
}
