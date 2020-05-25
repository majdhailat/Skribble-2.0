//Imports
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Client extends JFrame {
    private volatile boolean running = true;//If the client is running or not
    private DataPackage dataPackage;//the object that stores all game info that the client will ever need

    private volatile ArrayList<DrawingComponent> drawingComponents = new ArrayList<>();//the list of all individual

    private String usersTextMessage = null;//the most recent message that the user typed and pressed enter on
    private ArrayList<String> messagesToRender = new ArrayList<>();
    private boolean canReceiveMessages = false;//if the client can display other user's messages
    private int previousMessagesSize = 0;//keeps track of which messages were already rendered to the screen by index
    private boolean promptedStartMessage = false;

    public static void main(String[] args) {

        Client client = new Client();
        String hostName = "localhost";//local host means you the server is on the same machine. If the server is on a
        //different machine but on the same network you must replace this with the host name followed by .local
        //on windows the host name can be found by going to the cmd line and typing hostname
        int portNumber = 4445;
        try (Socket socket = new Socket(hostName, portNumber)) {//connecting to server
            new ClientInputThread(socket, client).start();
            try {
                TimeUnit.MILLISECONDS.sleep(1000);//5000
            } catch (InterruptedException e) {e.printStackTrace();}
            new ClientOutputThread(socket, client).start();
            new Gui(client);
            client.additionalSetup();
            while (client.isRunning()) {
                if (!client.promptedStartMessage && client.dataPackage.getGameStatus().equals(DataPackage.WAITINGTOSTART) && client.dataPackage.getPlayers().size() >= 2 && client.dataPackage.getPlayers().get(0) == client.dataPackage.getMyPlayer()) {
                    //prompting user to type start when they want to start the game
                    client.messagesToRender.add("#008000~Type start to start the game");
                    client.promptedStartMessage = true;
                }
            }//keeps thread alive

        } catch (UnknownHostException e) {
            System.err.println("Unknown Host");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Port error");
            System.exit(1);
        }
    }

    public boolean isRunning(){
        return running;
    }

    public void additionalSetup(){
        messagesToRender.add("#008000~Enter a user name");//prompting user for their name
    }

    public void updateDataPackage(DataPackage pack){
        dataPackage = pack;
        if (!dataPackage.getMyPlayer().isArtist()) {//checking if i am not the artist
            if (dataPackage.getDrawingComponents() != null) {
                //setting my drawing components to that of the server so that my canvas is being updated
                //from the server which is coming from the artist
                drawingComponents = new ArrayList<>(Arrays.asList(dataPackage.getDrawingComponents()));
            }
        }
        ArrayList<String> messages = dataPackage.getMyPlayer().getMessages();
        if (messages.size() > previousMessagesSize) {
            if (canReceiveMessages) {
                int numOfNewMessages = messages.size() - previousMessagesSize;
                for (int i = 0; i < numOfNewMessages; i++) {
                    String msg = messages.get(previousMessagesSize + i);
                    messagesToRender.add(msg);
                }
            }
            previousMessagesSize = messages.size();
        }
    }

    public DataPackage getDataPackage(){
        return dataPackage;
    }

    public DrawingComponent[] getDrawingComponents(){
        DrawingComponent[] drawingComponentsArray = new DrawingComponent[drawingComponents.size()];
        drawingComponentsArray = drawingComponents.toArray(drawingComponentsArray);
        return drawingComponentsArray;
    }

    public ArrayList<DrawingComponent> getDrawingComponentsArrayList(){
        return drawingComponents;
    }

    public void updateUsersTextMessage(String message){
        usersTextMessage = message;
    }

    public ArrayList<String> getMessagesToRender(){
        return messagesToRender;
    }

    public String getUsersTextMessage(){
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {e.printStackTrace();}
        String msg = usersTextMessage;
        usersTextMessage = null;
        if (msg != null) {
            if (!dataPackage.getMyPlayer().gotUserName()){
                canReceiveMessages = true;
                return msg;
            }
            else if (dataPackage.getGameStatus().equals(DataPackage.WAITINGTOSTART) && dataPackage.getPlayers().get(0) == dataPackage.getMyPlayer() && msg.equals("start")){
                return "/START";
            }else{
                messagesToRender.add("Me: " + msg);
                promptedStartMessage = false;
                return msg;
            }
        }else {
            return null;
        }
    }

    public static int randint(int low, int high){
        return (int)(Math.random()*(high-low+1)+low);
    }
}