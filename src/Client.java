//Imports
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Client extends JFrame {
    private volatile boolean isRunning = true;//If the client is running or not
    private DataPackage dataPackage;//the object that stores all game info that the client will need
    private volatile ArrayList<DrawingComponent> drawingComponents = new ArrayList<>();//the list of all pieces that make up the drawing

    private String usersTextMessage = null;//the most recent message that the user typed and pressed enter on
    private ArrayList<String> messagesToRender = new ArrayList<>();//queue of messages not on screen yet
    private boolean canReceiveMessages = false;//if the client can display other user's messages
    private int previousMessagesSize = 0;//keeps track of which messages were already rendered to the screen by index
    private boolean promptedStartMessage = false;//if the "type start" message was shown

    /*
    Connects to the server
    Starts the I/O threads for communication to the server
    Starts the graphics thread
     */
    public synchronized static void main(String[] args) {
        Client client = new Client();
        String hostName = "localhost";//HOST NAME
        int portNumber = 4445;//PORT NUMBER
        try (Socket socket = new Socket(hostName, portNumber)) {//connecting to server
            new ClientInputThread(socket, client).start();//starting client thread
            try {
                TimeUnit.MILLISECONDS.sleep(5000);//5000 default
            } catch (InterruptedException e) {e.printStackTrace();}
            new ClientOutputThread(socket, client).start();
            new Gui(client);
            client.additionalSetup();
            while (client.isRunning) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);//5000
                } catch (InterruptedException e) {e.printStackTrace();}
                client.whileRunning();
            }

        } catch (UnknownHostException e) {
            System.err.println("Unknown Host");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Port error");
            System.exit(1);
        }
    }

    /*
    returns isRunning
     */
    public synchronized boolean isRunning(){
        return isRunning;
    }

    /*
    any additional setup after the client boots up
     */
    public void additionalSetup() {
        messagesToRender.add("#008000~Enter a user name");//prompting user for their name
    }

    /*
    code that runs as long as the client is running
     */
    public void whileRunning(){
        if (!promptedStartMessage && dataPackage.getGameStatus().equals(DataPackage.WAITINGTOSTART) && dataPackage.getPlayers().size() >= 2 && dataPackage.getPlayers().get(0) == dataPackage.getMyPlayer()) {
            //prompting user to type start when they want to start the game
            messagesToRender.add("#008000~Type start to start the game");
            promptedStartMessage = true;
        }
    }

    /*
    takes the data package (from server -> server output thread -> client input thread -> client (here))
    updates the drawing components
    updates the messages and adds any new messages to the render queue
     */
    public void updateDataPackage(DataPackage pack){
        dataPackage = pack;
        if (!dataPackage.getMyPlayer().isArtist()) {//checking if not the artist
            if (dataPackage.getDrawingComponents() != null) {
                //setting my drawing components to that of the server so that my canvas is being updated
                //from the server which is coming from the artist
                drawingComponents = new ArrayList<>(Arrays.asList(dataPackage.getDrawingComponents()));
            }
        }
        ArrayList<String> messages = dataPackage.getMyPlayer().getMessages();
        if (messages.size() > previousMessagesSize) {//checking if there are any new messages
            if (canReceiveMessages) {
                int numOfNewMessages = messages.size() - previousMessagesSize;
                for (int i = 0; i < numOfNewMessages; i++) {//updating render queue with any new messages
                    String msg = messages.get(previousMessagesSize + i);
                    messagesToRender.add(msg);
                }
            }
            previousMessagesSize = messages.size();
        }
    }

    /*
    returns data package (to client output thread -> server input thread -> server)
     */
    public DataPackage getDataPackage(){
        return dataPackage;
    }

    /*
    returns array of drawing components
     */
    public DrawingComponent[] getDrawingComponents(){
        DrawingComponent[] drawingComponentsArray = new DrawingComponent[drawingComponents.size()];
        drawingComponentsArray = drawingComponents.toArray(drawingComponentsArray);
        return drawingComponentsArray;
    }

    /*
    returns array list of DC
     */
    public ArrayList<DrawingComponent> getDrawingComponentsArrayList(){
        return drawingComponents;
    }

    /*
    takes a message and sets the users message to it. the users message then gets picked up by the client output thread
    (keyboard -> gui -> client (here) -> client output thread -> server input thread -> server -> all other clients)
     */
    public void updateUsersTextMessage(String message){
        usersTextMessage = message;
    }

    /*
    returns array of messages to render
     */
    public ArrayList<String> getMessagesToRender(){
        return messagesToRender;
    }

    /*
    returns the users text message to the client output thread to send to server
    handles any commands
     */
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
                promptedStartMessage = false;
                return "/START";
            }else{
                messagesToRender.add("Me: " + msg);
                return msg;
            }
        }else {
            return null;
        }
    }

    /*
    returns rand int (high and low inclusive)
     */
    public static int randint(int low, int high){
        return (int)(Math.random()*(high-low+1)+low);
    }
}