//Imports
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

//Stores all the global information about the game and starts the listening thread
public class Server {
    private boolean running = true;//state of server

    private ShapeObject[] shapesArray = null;
    private String message;//the text message
    private Player artist;
    private ArrayList<Player> messageReaders = new ArrayList<Player>();//the players who read the text message
    private ArrayList<Player> players = new ArrayList<Player>();
    private ArrayList<Player> winners = new ArrayList<Player>();
    private String currentMagicWord; //should be a read from a txt file
    private ArrayList<String> magicWords = new ArrayList<String>();

    public static void main(String []args){
        Server server = new Server();
        new ListenForClients(server).start();
    }

    public boolean isRunning(){
        return running;
    }


    private boolean setArtist = false;
    public synchronized void newPlayer(Player player){

        players.add(player);
    }

    public synchronized void newMessage(String msg, Player player){
        messageReaders.clear();
        if (msg.contains("~")){//this means that the client is sending a message and not the user
            message = msg;
        }else {
            if(msg.equals(currentMagicWord)){
                winners.add(player);
                message = player.getName() + " has guessed correctly!";
                if(winners.size() == players.size()){
                    newRound();
                }
            }
            else{
                message = (player.getName()) + ": " + msg;
            }
        }
        messageReaders.add(player);
    }

    public synchronized void updateShapes(ShapeObject[] shapesArray) {
        this.shapesArray = shapesArray;
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
        return new DataPackage(getMessage(player), players, winners, player, artist, shapesArray);
    }

    public void newRound(){
        artist = players.get(randint(0, players.size()-1)); //or we could go through the list
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

    public static int randint(int low, int high){
        //this method returns a random int between the low and high args inclusive
        return (int)(Math.random()*(high-low+1)+low);
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
    private Socket socket;
    private Player player;
    private boolean gotUserName = false;
    private BufferedReader in;
    private Server server;

    private ObjectInputStream canvasIn;

    public ServerOutputThread(Server server, Player player, Socket socket) throws IOException {
        this.socket = socket;
        this.server = server;
        this.player = player;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        canvasIn = new ObjectInputStream(socket.getInputStream());
    }


    public void run() {
        try {
            while (server.isRunning()) {
                if (player.isArtist()) {
                    try {
                        ShapeObject[] shapesArray = (ShapeObject[])canvasIn.readObject();
                        if (shapesArray.length > 0) {

                            server.updateShapes(shapesArray);
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                } else {
                    String inputLine;
                    if ((inputLine = in.readLine()) != null) {
                        if (gotUserName) {

                            server.newMessage(inputLine, player);

                        } else if (inputLine.contains("USERNAME")) {
                            String[] splitInputLine = inputLine.split(" ");
                            player.setName(splitInputLine[1]);
                            server.newMessage(("#FF0000~" + splitInputLine[1] + " has joined the game"), player);
                            if (splitInputLine[1].equals("artist")) {
                                player.setArtist(true);
                            }
                            server.newPlayer(player);
                            gotUserName = true;
                        }
                    }
                }
            }

        } catch(IOException e){
            e.printStackTrace();
        } finally{
            server.playerDisconnected(player);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}




//Gets the data package from the server and sends it to the client
class ServerChatInputThread extends Thread{
    private Socket socket;
    private Server server;
    private Player player;
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
        catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
