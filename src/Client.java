//Imports
import javax.imageio.ImageIO;
import javax.sound.midi.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Client extends JFrame{
    private volatile boolean running = true;//If the client is running or not
    private DataPackage dataPackage;//the object that stores all game info that the client will ever need

    private volatile ArrayList<DrawingComponent> drawingComponents = new ArrayList<>();//the list of all individual
    //pieces that make up the drawing. It is iterated through in the GUI and each component in the
    //array is drawn onto the canvas.

    private String usersTextMessage = null;//the most recent message that the user typed and pressed enter on
    private ArrayList<String> messagesToRender = new ArrayList<>();
    private boolean canReceiveMessages = false;//if the client can display other user's messages
    //private ArrayList<String>messages = new ArrayList<>();//the list of messages for user
    private int previousMessagesSize = 0;//keeps track of which messages were already rendered to the screen by index
    //of the messages arrayList

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
            try {
                TimeUnit.MILLISECONDS.sleep(5000);
            } catch (InterruptedException e) {e.printStackTrace();}
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
            messagesToRender.add("#008000~Enter a user name");//prompting user for their name
        }

        //if the message that tells the user to type start has been prompted
        private boolean promptedStartMessage = false;
        public void run() {
            while (running) {
                if (dataPackage != null) {
                    if (!promptedStartMessage && dataPackage.getPlayers().size() >= 2 && dataPackage.getPlayers().get(0) == dataPackage.getMyPlayer()) {
                        //prompting user to type start when they want to start the game
                        messagesToRender.add("#008000~Type start to start the game");
                        promptedStartMessage = true;
                    }
                    //NON ARTIST MODE
                    if (!dataPackage.amIArtist() && usersTextMessage != null) {
                        try {
                            if (!gotUserName) {//checking if user name has not been obtained
                                canReceiveMessages = true;
                                out.writeObject(usersTextMessage);//sending the user name
                                gotUserName = true;
                                //checking if the user started the game
                            } else if (dataPackage.getPlayers().get(0) == dataPackage.getMyPlayer() && usersTextMessage.equals("start")) {
                                out.writeObject("/START");//a command alerting the server to start the game
                            } else {//the user is just sending a message
                                out.writeObject(usersTextMessage);//sending the users message to the server
                                //displaying the users message back to the chat panel
                                messagesToRender.add("Me: " + usersTextMessage);
                            }
                            //resetting the users text message so that it does'nt get resent to the server
                            usersTextMessage = null;
                            out.flush();
                        }catch(IOException e){e.printStackTrace();}
                    }
                    //ARTIST MODE
                    else if (dataPackage.amIArtist()){//checking if the user is an artist
                        drawingComponents.clear();
                        while(dataPackage.amIArtist()){//starting artist loop
                            try {
                                TimeUnit.MILLISECONDS.sleep(100);
                            } catch (InterruptedException e) {e.printStackTrace();}
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
                        try {
                            out.writeObject(0);//this is a "band aid" fix
                            /*
                            what it does is on the server side, the server is currently in a waiting stage because
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

                    ArrayList<String> messages = dataPackage.getMyPlayer().getMessages();
                    if (dataPackage.getMyPlayer().getMessages().size() > previousMessagesSize) {
                        if (canReceiveMessages) {
                            int numOfNewMessages = messages.size() - previousMessagesSize;
                            for (int i = 0; i < numOfNewMessages; i++) {
                                String msg = messages.get(previousMessagesSize + i);
                                messagesToRender.add(msg);
                            }
                        }
                        previousMessagesSize = messages.size();
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

    // ------------ Graphics ------------------------------------------
    //sets up JFrame and starts the JPanel
    public class Gui extends JFrame {
        private Panel panel;
        public Gui(){
            super("Skribble");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(1300, 740);
            Timer myTimer = new Timer(100, new TickListener());// trigger every 100 ms. used to refresh graphics
            myTimer.start();
            panel = new Panel();
            add(panel);
            setResizable(false);
            setVisible(true);
            //this line of code executes anything inside the curly brackets when the user closes the window
            //we are setting running to false to close all threads properly
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    running = false;
                }
            });
        }

        //triggered every 100 ms by myTimer
        class TickListener implements ActionListener {
            public void actionPerformed(ActionEvent evt) {
                if (panel != null && panel.ready) {
                    panel.repaint();
                }
            }
        }
    }

    public class Panel extends JPanel implements MouseListener, MouseMotionListener {
        public boolean ready = false;

        private JTextField textField = new JTextField();//the box in which the user can type their message
        private JList messageList = new JList(messagesToRender.toArray());
        private JScrollPane messagePane = new JScrollPane(messageList);

        public Panel() {
            //sets running to false when windows is closed to close all threads
            setLayout(null);//prevents any form of auto layout
            addMouseListener(this);//used to detect mouse actions
            addMouseMotionListener(this);//used to detect mouse dragging
            startMidi("bgmusic.mid");//starting music

            textField.setBounds(964, 590, 294, 22);
            textField.addKeyListener((KeyListener) new MKeyListener());
            add(textField);

            messageList.setCellRenderer(createListRenderer());
            messagePane.setBounds(958, 64, 312, 500);
            add(messagePane);
        }

        //I barley understand how this works so don't ask
        private ListCellRenderer<? super String> createListRenderer() {
            return new DefaultListCellRenderer() {
                private int previousMessagesToRenderSize = 0;
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (c instanceof JLabel) {
                        JLabel label = (JLabel) c;
                        String message = messagesToRender.get(index);
                        if (message.contains("~")){
                            String[] messageParts = message.split("~");
                            label.setForeground(Color.decode(messageParts[0]));
                            label.setText(messageParts[1]);
                        }else {
                            label.setForeground(Color.black);
                            label.setText(messagesToRender.get(index));
                        }
                        if (messagesToRender.size() > previousMessagesToRenderSize) {
                            messageList.setListData(messagesToRender.toArray());
                            JScrollBar sb = messagePane.getVerticalScrollBar();
                            sb.setValue(sb.getMaximum());
                            previousMessagesToRenderSize = messagesToRender.size();
                        }
                    }
                    return c;
                }
            };
        }

        public void addNotify() {
            super.addNotify();
            requestFocus();
            ready = true;
        }

        //takes music file path, loads music and plays it in loop
        public void startMidi(String midFilename) {
            try {
                File midiFile = new File(midFilename);
                Sequence song = MidiSystem.getSequence(midiFile);
                Sequencer midiPlayer = MidiSystem.getSequencer();
                midiPlayer.open();
                midiPlayer.setSequence(song);
                midiPlayer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
                midiPlayer.start();
            } catch (MidiUnavailableException | InvalidMidiDataException | IOException e) {
                e.printStackTrace();
            }
        }

        //the canvas rectangle where the image is drawn
        private Rectangle canvasPanel = new Rectangle(201, 64, 749, 562);
        //loading the color palette image
        private Image colorPickerImage = new ImageIcon("Color picker.png").getImage();
        //the rectangle around the color picker (not actually displayed, but used to check if the user clicks in it)
        private Rectangle colorPickerPanel = new Rectangle(260, 632, colorPickerImage.getWidth(null), colorPickerImage.getHeight(null));
        //loads tool images and creates rectangle objects to check for collision
        private Image pencilImage = new ImageIcon("pencil.png").getImage();
        private Image eraserImage = new ImageIcon("eraser.png").getImage();
        private Rectangle pencilPanel = new Rectangle(610, 632, pencilImage.getWidth(null), pencilImage.getHeight(null));
        private Rectangle eraserPanel = new Rectangle(675, 632, eraserImage.getWidth(null), eraserImage.getHeight(null));

        //renders the GUI and calls any methods relates to the GUI
        public void paintComponent(Graphics g) {
            if (g != null) {
                g.setColor(new Color(10, 180, 150));//background color
                g.fillRect(0, 0, getWidth(), getHeight());//background
                g.setColor(Color.white);
                g.fillRect((int) canvasPanel.getX(), (int) canvasPanel.getY(),//filling the canvas with white
                        (int) canvasPanel.getWidth(), (int) canvasPanel.getHeight());
                g.setColor(new Color(237, 237, 237));
                g.drawImage(colorPickerImage, (int) colorPickerPanel.getX(), (int) colorPickerPanel.getY(), null);
                //drawing the tool images
                g.drawImage(pencilImage, (int) pencilPanel.getX(), (int) pencilPanel.getY(), null);
                g.drawImage(eraserImage, (int) eraserPanel.getX(), (int) eraserPanel.getY(), null);

                //iterating through the drawing components and drawing each component onto the screen
                //basically drawing the image
                if (drawingComponents.size() > 0) {
                    Graphics2D g2 = (Graphics2D) g;
                    for (DrawingComponent s : drawingComponents) {
                        g2.setStroke(new BasicStroke(s.getStroke()));

                        g2.setColor(s.getCol());
                        g2.draw(new Line2D.Float(s.getX1(), s.getY1(), s.getX2(), s.getY2()));
                    }
                }

                updateTimerTextArea();
                updatePlayerTextAreas(g);
            }
        }

        private JTextArea timerText = new JTextArea();//the timer text box
        private boolean initializedTimerTextArea = false;

        //initializes the timer text box then continuously updates the timer if the timer is actually running
        public void updateTimerTextArea() {
            if (!initializedTimerTextArea) {
                timerText.setBounds(100, 100, 30, 22);
                timerText.setEditable(false);
                timerText.setVisible(false);
                add(timerText);
                initializedTimerTextArea = true;
            } else {
                if (dataPackage.getTimeRemaining() == -1) {//-1 is the null state (probably not a good idea)
                    timerText.setVisible(false);//hiding timer
                } else {
                    //showing timer (probably should'nt run this every time but idk what else to do)
                    timerText.setVisible(true);
                    timerText.setText("" + dataPackage.getTimeRemaining());//updating the timer text using data package
                }
            }
        }

        private JTextArea[] playerNameLabels = new JTextArea[8];//the text boxes that display the players names
        private boolean initializedPlayerNameLabels = false;
        private Font nameLabelFont = new Font("Arial", Font.PLAIN, 14);//font of all players except their self
        private Font myNameLabelFont = new Font("Arial", Font.BOLD, 14);//font for their own player

        //reads the array of players from the data package and displays boxes for each player on the left of the screen
        //boxes will include name, score, who the artist is and any new info needed further down the line
        public void updatePlayerTextAreas(Graphics g) {
            if (!initializedPlayerNameLabels) {
                //maximum of 8 players will ever be displayed - adding all 8 name labels to array
                for (int i = 0; i < 8; i++) {
                    JTextArea label = new JTextArea();
                    label.setVisible(false);
                    label.setEditable(false);
                    label.setBounds(12, 90 + (75 * i), 181, 30);
                    add(label);
                    playerNameLabels[i] = label;//adding to array
                }
                initializedPlayerNameLabels = true;
            }

            for (int i = 0; i < playerNameLabels.length; i++) {//iterating through name labels
                JTextArea label = playerNameLabels[i];//getting label
                if (i < dataPackage.getPlayers().size()) {//checking if there is a player for that label
                    g.setColor(dataPackage.getPlayers().get(i).getColor());//setting the color of the label to the
                    //players customized color specified in the player object
                    g.fillRect(10, 64 + (75 * i), 183, 70);//filling the box around the stats
                    label.setBackground(dataPackage.getPlayers().get(i).getColor());//setting the label background
                    label.setAlignmentX(CENTER_ALIGNMENT);
                    if (dataPackage.getPlayers().get(i) == dataPackage.getMyPlayer()) {//checking if its the users player
                        label.setFont(myNameLabelFont);//setting bolded font
                        label.setText(dataPackage.getPlayers().get(i).getName() + " (You)   " + dataPackage.getPlayers().get(i).getScore());//setting name as text
                    } else {
                        label.setFont(nameLabelFont);//setting non bolded font
                        label.setText(dataPackage.getPlayers().get(i).getName() + "   " + dataPackage.getPlayers().get(i).getScore());//setting name as text
                    }
                    label.setVisible(true);
                    label.setForeground(Color.black);//text color
                } else {//this is a label that does not have a player
                    if (label.isVisible()) {//checking if the label is visible (this only happens after a player DCs)
                        label.setVisible(false);//hiding it
                    }
                }
            }
        }

        // ------------ MouseListener ------------------------------------------
        //I WILL ADD COMMENTS LATER BECAUSE THERE IS A LOT MORE CODE TO ADD HERE
        private int x1, y1, x2, y2;

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            x1 = e.getX();
            y1 = e.getY();
            if (colorPickerPanel.contains(x1, y1)) {
                try {
                    BufferedImage image = ImageIO.read(new File("Color picker.png"));
                    Color c = new Color(image.getRGB((int) (x1 - colorPickerPanel.getX()), (int) (y1 - colorPickerPanel.getY())));
                    DrawingComponent.setColor(c);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else if (pencilPanel.contains(x1, y1)) {
                DrawingComponent.setToolType("PENCIL");
            } else if (eraserPanel.contains(x1, y1)) {
                DrawingComponent.setToolType("ERASER");
            }
        }

        synchronized public void mouseDragged(MouseEvent e) {
            x2 = e.getX();
            y2 = e.getY();

            if (canvasPanel.contains(x1, y1) && dataPackage.amIArtist()) {
                if (DrawingComponent.getToolType().equals(DrawingComponent.PENCIL) || DrawingComponent.getToolType().equals(DrawingComponent.ERASER)) {
                    if (x2 < canvasPanel.getX()) {
                        x2 = (int) canvasPanel.getX();
                    } else if (x2 > canvasPanel.getX() + canvasPanel.getWidth()) {
                        x2 = (int) (canvasPanel.getX() + canvasPanel.getWidth());
                    }
                    if (y2 < canvasPanel.getY()) {
                        y2 = (int) canvasPanel.getY();
                    } else if (y2 > canvasPanel.getY() + canvasPanel.getHeight()) {
                        y2 = (int) (canvasPanel.getY() + canvasPanel.getHeight());
                    }
                    drawingComponents.add(new DrawingComponent(x1, y1, x2, y2));

                }
            }
            x1 = x2;
            y1 = y2;
        }

        public void mouseMoved(MouseEvent e) {
        }

        class MKeyListener extends KeyAdapter {
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (!textField.getText().equals("")) {
                        usersTextMessage = textField.getText();
                        textField.setText("");
                    }
                }
            }
        }
    }
}

