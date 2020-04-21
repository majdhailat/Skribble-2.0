//Imports
import javax.imageio.ImageIO;
import javax.sound.midi.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Client extends JFrame {
    private volatile boolean running = true;//If the client is running or not
    private DataPackage dataPackage;//the object that stores all game info that the client will ever need

    private boolean madeChangesToDrawing = false;//if the user has added anything to the drawing since the last time
    //it has been updated to the server
    private volatile ArrayList<DrawingComponent> drawingComponents = new ArrayList<>();//the list of all individual
    //pieces that make up the drawing. It is iterated through in the GUI and each component in the
    //array is drawn onto the canvas.

    private String usersTextMessage = null;//the most recent message that the user typed and pressed enter on
    private String [] messageQueue = new String[17];//the past 17 messages displayed for the user in the chat panel
    private boolean canReceiveMessages = false;//if the client can display other user's messages

    public static void main(String[] args)  {
        new Client();
    }

    public Client(){
        String hostName = "localhost";//local host means you the server is on the same machine. If the server is on a
        //different machine but on the same network you must replace this with the host name followed by .local
        //on windows the host name can be found by going to the cmd line and typing hostname
        int portNumber = 4445;
        try (Socket socket = new Socket(hostName, portNumber)){//connecting to server
            new InputThread(socket).start();
            new OutputThread(socket).start();
            new Gui();
            while (running) {
                Thread.onSpinWait();//keeps thread alive
            }
        } catch (UnknownHostException e) {
            System.err.println("Unknown Host");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Port error");
            System.exit(1);
        }
    }

    // ------------ Networking ------------------------------------------
    /*
    Sends output to server

    If the client is an artist this thread sends the drawing components (the canvas instructions) to the server
    input thread which then updates it to the server

    If the client is not an artist this thread simply sends messages to the server input thread these messages are
    either commands from the client or they are the users text message which is obtained from the user text message
    string which is updated when the user clicks enter
    */
    public class OutputThread extends Thread {
        //The stream is used to send both the drawing components and the users string message
        private ObjectOutputStream out;
        private boolean gotUserName = false;//if the player has input their user name

        public OutputThread(Socket socket) throws IOException {
            out = new ObjectOutputStream(socket.getOutputStream());
            addMessageToQueue("#008000~Enter a user name");//prompting user for their name
        }

        //if the message that tells the user to type start has been prompted
        private boolean promptedStartMessage = false;
        public void run() {
            while (running) {
                if (dataPackage != null) {
                    if (!promptedStartMessage && dataPackage.getPlayers().size() >= 2 && dataPackage.getPlayers().get(0) == dataPackage.getMyPlayer()) {
                        //prompting user to type start when they want to start the game
                        addMessageToQueue("#008000~Type start to start the game");
                        promptedStartMessage = true;
                    }
                    //NON ARTIST MODE
                    if (!dataPackage.amIArtist() && usersTextMessage != null) {
                        try {
                            if (!gotUserName) {//checking if user name has not been obtained
                                out.writeObject(usersTextMessage);//sending the user name
                                gotUserName = true;
                                canReceiveMessages = true;
                                //checking if the user started the game
                            } else if (dataPackage.getPlayers().get(0) == dataPackage.getMyPlayer() && usersTextMessage.equals("start")) {
                                out.writeObject("START");
                            } else {//the user is just sending a message
                                out.writeObject(usersTextMessage);//sending the users message to the server
                                //displaying the users message back to the chat panel
                                addMessageToQueue("Me: " + usersTextMessage);
                            }
                            //resetting the users text message so that it does'nt get resent to the server
                            usersTextMessage = null;
                            out.flush();
                        }catch(IOException e){e.printStackTrace();}
                    }
                    //ARTIST MODE
                    else if (dataPackage.amIArtist()){//checking if the user is an artist
                        while(dataPackage.amIArtist()){//starting artist loop
                            try {
                                TimeUnit.MILLISECONDS.sleep(100);
                            } catch (InterruptedException e) {e.printStackTrace();}
                            if(madeChangesToDrawing) {
                                madeChangesToDrawing = false;
                                try {
                                    //converting drawing components from arrayList to array because there were
                                    //issues serializing an array list for a reason that I forgot
                                    DrawingComponent[] drawingComponentsArray = new DrawingComponent[drawingComponents.size()];
                                    drawingComponentsArray = drawingComponents.toArray(drawingComponentsArray);
                                    out.writeObject(drawingComponentsArray);//sending drawing components array
                                    out.flush();
                                    out.reset();//may be unnecessary
                                } catch (IOException e) {e.printStackTrace();}
                            }
                        }
                        try {
                            out.writeObject(0);//this is a "band aid" fix
                            /*
                            but what it does is on the server side, the server is currently in a waiting stage because
                            it is waiting for a drawing components array but since this loop just ended the next thing it will get is a message
                            that will cause the server to throw a class cast exception because it tried casting a string to an array
                            this exception can be ignored harmlessly but it will mean that the message never makes it to the server
                            so this out statement will trigger the exception and get it over with so that the message that is coming up will
                            not be the thing to cause the exception thus it will make it to the server
                            */
                        } catch (IOException e) {e.printStackTrace();}
                    }
                }
            }
        }
    }

    /*
    Gets input from server:

    this class gets the data package from the server and then updates the data package filed in the client
    so that the rest of the client processes have all the info from the server that they need

    after receiving the data package this class also updates the drawing components list as well as any new message
     */
    public class InputThread extends Thread {
        private ObjectInputStream objectInputStream;//this stream is used to read the data package object
        //the messages that have been read from the messages only for me list in the player
        private ArrayList<String> readMessagesOnlyForMe = new ArrayList<>();

        public InputThread(Socket socket) throws IOException {
            objectInputStream = new ObjectInputStream(socket.getInputStream());
        }

        public void run() {
            try {
                while (running) {
                    //WAITING for the server to send the data package then reading it from the server
                    dataPackage = (DataPackage) objectInputStream.readUnshared();
                    //checking if the server has cleared the canvas -> clearing my canvas
                    if (dataPackage.getDrawingComponents() == null){
                        drawingComponents.clear();
                    }
                    if (!dataPackage.amIArtist()) {//checking if i am not the artist
                        if (dataPackage.getDrawingComponents() != null) {
                            //setting my drawing components to that of the server so that my canvas is being updated
                            //from the server which is coming from the artist
                            drawingComponents = new ArrayList<>(Arrays.asList(dataPackage.getDrawingComponents()));
                        }
                    }

                    String message = dataPackage.getMessage();//getting message from the data package
                    if (message != null && canReceiveMessages) {
                        addMessageToQueue(message);//adding message to the chat panel
                    }

                    //getting messages that are only for this player
                    ArrayList<String> messageOnlyForMe = dataPackage.getMyPlayer().getMessageOnlyForMe();
                    //checking if there are any unread messages
                    if (messageOnlyForMe.size() > readMessagesOnlyForMe.size()){
                        //getting the num of un read messages
                        int numOfNewMessages = messageOnlyForMe.size() - readMessagesOnlyForMe.size();
                        for (int i = 0; i < numOfNewMessages; i++){
                            //adding messages
                            addMessageToQueue(messageOnlyForMe.get(readMessagesOnlyForMe.size() + i));
                        }
                        //updating read messages
                        readMessagesOnlyForMe = messageOnlyForMe;
                    }
                    /*
                    The reason why we have to store the read messages here on the client rather than just removing
                    them from the array list inside of the player is because when the client modifies the player object
                    it never gets modified on the server because when transmitting the object its as if we did a deep
                    copy so, the next time we check the clients messages it will be as if we never read any because
                    again, the client CANNOT modify the player object in any way, if it does it only happens locally and
                    is totally useless
                     */
                }
            }catch (IOException | ClassNotFoundException e) {e.printStackTrace();}
        }
    }

    //Takes a message and adds it to the array of the past 17 messages, when the array is full
    //and a new message is added it shift all messages back deleting the oldest message from queue
    private boolean messageQueueIsFull = false;//if the chat panel has filled up and now we have to start deleting
    //the 17th last message to make room for the new message
    public void addMessageToQueue(String message){
        if (!messageQueueIsFull) {
            boolean messageWasAdded = false;
            for (int i = 0; i < messageQueue.length; i++) {//iterating through the 17 spots
                if (messageQueue[i] == null) {//checking if there is an empty spot
                    messageQueue[i] = message;//setting the message to that spot
                    messageWasAdded = true;
                    break;
                }
            }
            if (!messageWasAdded){//there were no empty spots
                messageQueueIsFull = true;
            }
        }
        else{
            messageQueue[0] = null;//deleting the last message
            //shifting all messages back
            System.arraycopy(messageQueue, 1, messageQueue, 0, messageQueue.length - 1);
            messageQueue[messageQueue.length - 1] = message;//adding the new message to the front of the queue
        }
    }

    // ------------ Graphics ------------------------------------------
    public class Gui extends JFrame {
        private Panel panel;
        public Gui(){
            super("Skribble");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(1300, 740);
            Timer myTimer = new Timer(100, new TickListener());// trigger every 100 ms
            myTimer.start();
            panel = new Panel();
            add(panel);
            setResizable(false);
            setVisible(true);
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    running = false;
                }
            });
        }

        class TickListener implements ActionListener {
            public void actionPerformed(ActionEvent evt) {
                if (panel != null && panel.ready) {
                    panel.repaint();
                }
            }
        }
    }

    public class Panel extends JPanel implements MouseListener, MouseMotionListener{
        public boolean ready = false;
        private JTextArea timerText = new JTextArea();

        public Panel(){
            //sets running to false when windows is closed to close all threads
            setLayout(null);
            addMouseListener(this);
            addMouseMotionListener(this);
            startMidi("bgmusic.mid");
        }

        public void addNotify() {
            super.addNotify();
            requestFocus();
            ready = true;
        }

        public void startMidi(String midFilename) {
            try {
                File midiFile = new File(midFilename);
                Sequence song = MidiSystem.getSequence(midiFile);
                Sequencer midiPlayer = MidiSystem.getSequencer();
                midiPlayer.open();
                midiPlayer.setSequence(song);
                midiPlayer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY); // repeat 0 times (play once)
                midiPlayer.start();
            } catch (MidiUnavailableException | InvalidMidiDataException | IOException e) {
                e.printStackTrace();
            }
        }

        private Rectangle canvasPanel = new Rectangle(201, 64, 749, 562);
        private Rectangle chatPanel = new Rectangle(958, 64, 312, 562);
        private Image colorPickerImage = new ImageIcon("Color picker.png").getImage();
        private Rectangle colorPickerPanel = new Rectangle(260, 632, colorPickerImage.getWidth(null), colorPickerImage.getHeight(null));
        public void paintComponent(Graphics g) {
            if (g != null) {
                g.setColor(new Color(10, 180, 150));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.white);
                g.fillRect((int) canvasPanel.getX(), (int) canvasPanel.getY(),
                        (int) canvasPanel.getWidth(), (int) canvasPanel.getHeight());
                g.setColor(Color.black);
                g.setColor(new Color(237, 237, 237));
                g.fillRect((int) chatPanel.getX(), (int) chatPanel.getY(),
                        (int) chatPanel.getWidth(), (int) chatPanel.getHeight());
                g.drawImage(colorPickerImage, (int)colorPickerPanel.getX(), (int)colorPickerPanel.getY(), null);

                if (drawingComponents.size() > 0) {
                    for (DrawingComponent s : drawingComponents) {
                        g.setColor(s.getCol());
                        g.drawLine(s.getX1(), s.getY1(), s.getX2(), s.getY2());
                    }
                }

                updateTimerTextArea();
                updateMessageTextAreas();
                updatePlayerTextAreas(g);
            }
        }

        private boolean initializedTimerTextArea = false;
        public void updateTimerTextArea(){
            if (!initializedTimerTextArea){
                timerText.setBounds(100, 100, 30, 22);
                timerText.setEditable(false);
                timerText.setVisible(false);
                add(timerText);
                initializedTimerTextArea = true;
            }else {
                if (dataPackage.getTimeRemaining() == -1) {
                    timerText.setVisible(false);
                }else{
                    timerText.setVisible(true);
                    timerText.setText(""+dataPackage.getTimeRemaining());
                }
            }
        }
        
        //the text boxes that display the messages from queue
        private JTextArea[] messageTextAreas = new JTextArea[messageQueue.length];
        private JTextField textField = new JTextField();//the box in which the user can type their message
        private boolean initializedMessageTextAreas = false;
        //reads the message queue and sets the message text area with the right text
        public void updateMessageTextAreas(){
            if (!initializedMessageTextAreas) {
                textField.setBounds(964, 590, 294, 22);
                textField.addKeyListener((KeyListener) new MKeyListener());
                add(textField);
                for (int i = 0; i < messageQueue.length; i++) {
                    JTextArea txtArea = new JTextArea();
                    txtArea.setVisible(false);
                    txtArea.setEditable(false);
                    txtArea.setBounds(964, 73+(30*i), 300, 20);
                    add(txtArea);
                    messageTextAreas[i] = txtArea;
                }
                initializedMessageTextAreas = true;
            }

            for (int i = 0; i < messageQueue.length; i++) {
                String message = messageQueue[i];
                Color col = Color.black;
                if (messageQueue[i] != null){
                    if (messageQueue[i].contains("~")){
                        String [] messageParts = messageQueue[i].split("~");
                        col = Color.decode(messageParts[0]);
                        message = messageParts[1];
                    }

                    int numOfChars = message.length();
                    int maxCharsPerLine = 38;//the # of characters a single message can be until it must be wrapped
                    //message wrapping
                    if (numOfChars > maxCharsPerLine) {
                        int numOfWraps = (int) Math.ceil((double) numOfChars / (double) maxCharsPerLine);
                        for (int j = 0; j < numOfWraps; j++) {
                            String line = message.substring((maxCharsPerLine * j),
                                    Math.min(((maxCharsPerLine * j) + maxCharsPerLine), message.length()));
                            if (j == 0){
                                messageQueue[i] = line;
                            }else {
                                addMessageToQueue(line);
                            }
                        }
                    }
                    messageTextAreas[i].setVisible(true);
                    messageTextAreas[i].setText(message);
                    messageTextAreas[i].setForeground(col);
                }
            }
        }

        private JTextArea[] playerNameLabels = new JTextArea[8];//the text boxes that display the players names
        private boolean initializedPlayerNameLabels = false;
        private Font nameLabelFont = new Font("Arial", Font.PLAIN,14);
        private Font myNameLabelFont = new Font("Arial", Font.BOLD,14);
        //reads the array of players from the data package and displays boxes for each player on the left of the screen
        public void updatePlayerTextAreas(Graphics g){
            if (!initializedPlayerNameLabels){
                for (int i = 0; i < 8; i++) {
                    JTextArea label = new JTextArea();
                    label.setVisible(false);
                    label.setEditable(false);
                    label.setBounds(12, 90 +  (75 * i), 181, 30);
                    add(label);
                    playerNameLabels[i] = label;
                }
                initializedPlayerNameLabels = true;
            }

           for (int i = 0; i < playerNameLabels.length; i++){
               JTextArea label = playerNameLabels[i];
               if (i < dataPackage.getPlayers().size()){
                   g.setColor(dataPackage.getPlayers().get(i).getColor());
                   g.fillRect(10, 64 + (75 * i), 183, 70);
                   label.setBackground(dataPackage.getPlayers().get(i).getColor());
                   label.setAlignmentX(CENTER_ALIGNMENT);
                   if (dataPackage.getPlayers().get(i) == dataPackage.getMyPlayer()){
                       label.setFont(myNameLabelFont);
                       label.setText(dataPackage.getPlayers().get(i).getName()+" (You)");
                   }else{
                       label.setText(dataPackage.getPlayers().get(i).getName());
                       label.setFont(nameLabelFont);
                   }
                   label.setVisible(true);
                   label.setForeground(Color.black);
               }
               else {
                   if (label.isVisible()) {
                       label.setVisible(false);
                   }
               }
           }
        }

        // ------------ MouseListener ------------------------------------------
        private int x1, y1, x2, y2;
        public void mouseEntered(MouseEvent e) {}
        public void mouseExited(MouseEvent e) {}
        public void mouseReleased(MouseEvent e) {}
        public void mouseClicked(MouseEvent e) {}
        public void mousePressed(MouseEvent e) {
            x1 = e.getX();
            y1 = e.getY();
            if (colorPickerPanel.contains(x1, y1)){
                try {
                    BufferedImage image = ImageIO.read(new File("Color picker.png"));
                    Color c = new Color(image.getRGB((int)(x1-colorPickerPanel.getX()), (int)(y1-colorPickerPanel.getY())));
                    DrawingComponent.setColor(c);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        synchronized public void mouseDragged(MouseEvent e) {
            x2 = e.getX();
            y2 = e.getY();

            if (canvasPanel.contains(x1, y1) && dataPackage.amIArtist()) {
                madeChangesToDrawing = true;
                if (DrawingComponent.getToolType().equals(DrawingComponent.PENCIL)) {
                    if (x2 < canvasPanel.getX()){
                        x2 = (int) canvasPanel.getX();
                    }else if (x2 > canvasPanel.getX() + canvasPanel.getWidth()){
                        x2 = (int) (canvasPanel.getX() + canvasPanel.getWidth());
                    }
                    if (y2 < canvasPanel.getY()){
                        y2 = (int) canvasPanel.getY();
                    }else if (y2 > canvasPanel.getY() + canvasPanel.getHeight()){
                        y2 = (int) (canvasPanel.getY() + canvasPanel.getHeight());
                    }
                    drawingComponents.add(new DrawingComponent(x1, y1, x2, y2));

                }
            }
            x1 = x2;
            y1 = y2;
        }

        public void mouseMoved(MouseEvent e) {}

        class MKeyListener extends KeyAdapter {
            public void keyPressed(KeyEvent event) {
                if(event.getKeyCode() == KeyEvent.VK_ENTER){
                    usersTextMessage = textField.getText();
                    textField.setText("");
                }
            }
        }
    }
}

