//imports
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
    private volatile boolean running = true;//state of client
    private DataPackage dataPackage;//the object that stores all game info that the client will ever need
    private volatile ArrayList<DrawingComponent> drawingComponents = new ArrayList<>();
    private boolean canReceiveMessages = false;//if the client can display other user's messages
    private String [] messageQueue = new String[17];//the past 17 messages
    private String usersTextMessage = null;//the message that the user types

    public static void main(String[] args)  {
        new Client();
    }

    public Client(){
        String hostName = "localhost";
        int portNumber = 4445;
        try (Socket socket = new Socket(hostName, portNumber)){
            new ClientInputThread(socket).start();
            new ClientOutputThread(socket).start();
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
    //Reads the message from the user and sends it to the server
    public class ClientOutputThread extends Thread {
        private ObjectOutputStream out;
        private boolean gotUserName = false;

        public ClientOutputThread(Socket socket) throws IOException {
            out = new ObjectOutputStream(socket.getOutputStream());
            addMessageToQueue("#008000~Enter a user name");
        }

        private boolean promptedStartMessage = false;
        public void run() {
            while (running) {

                if (dataPackage != null) {
                    if (!promptedStartMessage && dataPackage.getPlayers().size() >= 2 && dataPackage.getPlayers().get(0) == dataPackage.getMyPlayer()) {
                        addMessageToQueue("#008000~Type start to start the game");
                        promptedStartMessage = true;
                    }
                    if (!dataPackage.amIArtist() && usersTextMessage != null) {
                        try {
                            if (!gotUserName) {
                                out.writeObject(usersTextMessage);
                                addMessageToQueue("#008000~Welcome to Skribble " + usersTextMessage);
                                gotUserName = true;
                                canReceiveMessages = true;
                            } else if (dataPackage.getGameState().equals(DataPackage.WAITINGFORPLAYERS) && usersTextMessage.equals("start")) {
                                out.writeObject("START");
                            } else {
                                out.writeObject(usersTextMessage);
                                addMessageToQueue("Me: " + usersTextMessage);
                            }
                            usersTextMessage = null;
                            out.flush();
                        }catch(IOException e){
                            e.printStackTrace();
                        }
                    }
                    else if (dataPackage.amIArtist()){
                        while(dataPackage.amIArtist()){
                            try {
                                TimeUnit.MILLISECONDS.sleep(1);
                            } catch (InterruptedException e) {e.printStackTrace();}
                            if(dataPackage.getDrawingComponents() == null || (drawingComponents.size() != dataPackage.getDrawingComponents().length)) {
                                try {
                                    DrawingComponent[] drawingComponentsArray = new DrawingComponent[drawingComponents.size()];
                                    drawingComponentsArray = drawingComponents.toArray(drawingComponentsArray);
                                    out.writeObject(drawingComponentsArray);
                                    out.flush();
                                    out.reset();

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    //Reads the data package from the server and stores it for the rest of the client to use
    public class ClientInputThread extends Thread {
        private ObjectInputStream objectInputStream;

        public ClientInputThread(Socket socket) throws IOException {
            objectInputStream = new ObjectInputStream(socket.getInputStream());
        }

        public void run() {
            try {
                while (running) {
                    dataPackage = (DataPackage) objectInputStream.readUnshared();
                    if (!dataPackage.amIArtist() && dataPackage.getDrawingComponents() != null) {
                        drawingComponents = new ArrayList<>(Arrays.asList(dataPackage.getDrawingComponents()));
                    }

                    String message = dataPackage.getMessage();
                    if (message != null && canReceiveMessages) {
                        addMessageToQueue(message);
                    }
                }
            }catch (IOException | ClassNotFoundException e) {e.printStackTrace();}
        }
    }

    //Takes a message and adds it to the array of the past 17 messages, when the array is full
    //and a new message is added it shift all messages back deleting the oldest message from queue
    private boolean messageQueueIsFull = false;
    public void addMessageToQueue(String message){
        if (!messageQueueIsFull) {
            boolean messageWasAdded = false;
            for (int i = 0; i < messageQueue.length; i++) {
                if (messageQueue[i] == null) {
                    messageQueue[i] = message;
                    messageWasAdded = true;
                    break;
                }
            }
            if (!messageWasAdded){
                messageQueueIsFull = true;
            }
        }
        else{
            messageQueue[0] = null;
            //shifting all messages back
            System.arraycopy(messageQueue, 1, messageQueue, 0, messageQueue.length - 1);
            messageQueue[messageQueue.length - 1] = message;
        }
    }

    // ------------ Graphics ------------------------------------------
    public class Gui extends JFrame {
        private Panel panel;
        public Gui(){
            super("Skribble");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(1280, 720);
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

                /*
                if (!dataPackage.getMyPlayer().isArtist() && dataPackage != null && dataPackage.getDrawingComponents() != null){
                    if (dataPackage.getDrawingComponents().length > 0) {
                        for (DrawingComponent s : dataPackage.getDrawingComponents()) {
                            g.setColor(s.getCol());
                            g.drawLine(s.getX1(), s.getY1(), s.getX2(), s.getY2());
                        }
                    }
                }else {

                 */
                    if (drawingComponents.size() > 0) {
                        for (DrawingComponent s : drawingComponents) {
                            g.setColor(s.getCol());
                            g.drawLine(s.getX1(), s.getY1(), s.getX2(), s.getY2());
                        }
                    }
                //}

                updateMessageTextAreas();
                updatePlayerTextAreas(g);
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
                File file= new File("your_file.jpg");
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

            if (canvasPanel.contains(x1, y1)) {
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

