import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Server {
    private boolean running = true;
    private String message;
    private ArrayList<Player> messageReaders = new ArrayList<>();
    private ArrayList<Player> players = new ArrayList<>();

    public static void main(String []args){
        Server server = new Server();
        new ListenForClients(server).start();
    }

    public synchronized void newPlayer(Player player){
        players.add(player);
    }

    public synchronized void newMessage(String msg, Player player){
        messageReaders.clear();
        if (msg.contains("~")){
            message = msg;
        }else {
            message = (player.getName()) + ": " + msg;
        }
        messageReaders.add(player);
    }

    public synchronized String getMessage(Player player) {
        if (messageReaders.contains(player)){
            return null;
        }else{
            messageReaders.add(player);
            return message;
        }
    }

    public synchronized ArrayList<Player> getPlayers(){
        return players;
    }

    public synchronized DataPackage getDataPackage(Player player){
        try {
            TimeUnit.MILLISECONDS.sleep(1);
        } catch (InterruptedException e) {e.printStackTrace();}
        return new DataPackage(getMessage(player), players, player);
    }

    public boolean isRunning(){
        return running;
    }
}

// ------------ Threads ------------------------------------------
class ListenForClients extends Thread{
    Server server;
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

class ServerOutputThread extends Thread{
    private Player player;
    private boolean gotUserName = false;
    private BufferedReader in;
    private Server server;
    public ServerOutputThread(Server server, Player player, Socket socket) throws IOException {
        this.server = server;
        this.player = player;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }
    public void run(){
        while (server.isRunning()) {
            try {
                String inputLine;
                if ((inputLine = in.readLine()) != null) {
                    if (gotUserName){
                        server.newMessage(inputLine, player);
                    }
                    else if (inputLine.contains("USERNAME")){
                        String[]splitInputLine = inputLine.split(" ");
                        player.setName(splitInputLine[1]);
                        server.newPlayer(player);
                        gotUserName = true;
                    }
                }
            } catch (IOException e) {e.printStackTrace();}
        }
    }
}

class ServerChatInputThread extends Thread{
    private Server server;
    private Player player;
    private ObjectOutputStream objectOutputStream;
    public ServerChatInputThread(Server server, Player player, Socket socket) throws IOException {
        this.server = server;
        this.player = player;
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
    }
    public void run(){
        while (server.isRunning()) {
            try {
                DataPackage dataPackage = server.getDataPackage(player);
                objectOutputStream.writeUnshared(dataPackage);
                objectOutputStream.flush();
                objectOutputStream.reset();
            } catch (IOException e) {e.printStackTrace();}
        }
    }
}
