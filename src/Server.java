import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Server {
    private static String message;
    private static ArrayList<Player> messageReaders = new ArrayList<>();
    private volatile static ArrayList<Player> players = new ArrayList<>();

    public static void main(String[] args) {
        int portNumber = 4444;
        boolean listening = true;
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            while (listening) {
                Socket socket = serverSocket.accept();
                Player player = new Player("");
                players.add(player);
                new ServerChatInputThread(player, socket).start();
                new ServerOutputThread(player, socket).start();
            }
        } catch (IOException e) {
            System.err.println("Port error");
            System.exit(-1);
        }
    }

    public static void newMessage(String msg, Player player){
        messageReaders.clear();
        if (msg.contains("~")){
            message = msg;
        }else {
            message = (player.getName()) + ": " + msg;
        }
        messageReaders.add(player);
    }

    public static String getMessage(Player player) {
        if (messageReaders.contains(player)){
            return null;
        }else{
            messageReaders.add(player);
            return message;
        }
    }

    public static ArrayList<Player> getPlayers(){
        return players;
    }

    public synchronized static DataPackage getDataPackage(Player player){
        try {
            TimeUnit.MILLISECONDS.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new DataPackage(Server.getMessage(player), players);

    }
}

// ------------ Threads ------------------------------------------

class ServerOutputThread extends Thread{
    private Player player;
    private boolean gotUserName = false;
    private BufferedReader in;
    public ServerOutputThread(Player player, Socket socket) throws IOException {
        this.player = player;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }
    public void run(){
        while (true) {
            try {
                String inputLine;
                if ((inputLine = in.readLine()) != null) {
                    if (gotUserName){
                        Server.newMessage(inputLine, player);
                    }
                    else if (inputLine.contains("USERNAME")){
                        String[]splitInputLine = inputLine.split(" ");
                        player.setName(splitInputLine[1]);
                        gotUserName = true;
                    }
                }
            } catch (IOException e) {e.printStackTrace();}
        }
    }
}

class ServerChatInputThread extends Thread{
    private Player player;
    private ObjectOutputStream objectOutputStream;
    public ServerChatInputThread(Player player, Socket socket) throws IOException {
        this.player = player;
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
    }
    public void run(){
        while (true) {
            try {
                objectOutputStream.writeObject(Server.getDataPackage(player));////THIS IS WHERE THE ISSUE OCCURS.... IS IT A SERIALIZATION ISSUE????
                objectOutputStream.flush();
            } catch (IOException e) {e.printStackTrace();}
        }
    }
}
