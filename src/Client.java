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
            while (running){
                if (dataPackage != null) {
                    if (!promptedStartMessage && dataPackage.getGameStatus().equals(DataPackage.WAITINGTOSTART) && dataPackage.getPlayers().size() >= 2 && dataPackage.getPlayers().get(0) == dataPackage.getMyPlayer()) {
                        //prompting user to type start when they want to start the game
                        messagesToRender.add("#008000~Type start to start the game");
                        promptedStartMessage = true;
                    }

                    //NON ARTIST MODE
                    if (!dataPackage.getMyPlayer().isArtist() && usersTextMessage != null) {
                        try {
                            if (!gotUserName) {//checking if user name has not been obtained
                                canReceiveMessages = true;
                                out.writeObject(usersTextMessage);//sending the user name
                                gotUserName = true;
                                //checking if the user started the game
                            } else if (dataPackage.getGameStatus().equals(DataPackage.WAITINGTOSTART) && dataPackage.getPlayers().get(0) == dataPackage.getMyPlayer() && usersTextMessage.equals("start")) {
                                out.writeObject("/START");//a command alerting the server to start the game

                            } else {//the user is just sending a message
                                promptedStartMessage = false;
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
                    else if (dataPackage.getMyPlayer().isArtist()){//checking if the user is an artist
                        //drawingComponents.clear();
                        while(dataPackage.getMyPlayer().isArtist() && dataPackage.getGameStatus().equals(DataPackage.ROUNDINPROGRESS)){//starting artist loop
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
                            usersTextMessage = null;
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
                    //if (dataPackage.getDrawingComponents() == null){
                        //drawingComponents.clear();
                    //}

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
            setSize(new Dimension(1280, 720));
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
        Font textFont;{
            try {
                textFont = Font.createFont(Font.TRUETYPE_FONT, new File("tipper.otf")).deriveFont(15.5f);
            } catch (FontFormatException | IOException e) {e.printStackTrace();}
        }

        private JTextArea timerText = new JTextArea();//the timer text box

        private JTextField textField = new JTextField();//the box in which the user can type their message

        private JList messageList = new JList(messagesToRender.toArray());
        private JScrollPane messagePane = new JScrollPane(messageList);
        private JScrollBar messagePaneScrollBar = messagePane.getVerticalScrollBar();

        private JList playerList = new JList(dataPackage.getPlayers().toArray());
        private JScrollPane playerPane = new JScrollPane(playerList);

        private JList pointsGainedNameList = new JList(dataPackage.getPlayers().toArray());
        private JScrollPane pointsGainedNamePane = new JScrollPane(pointsGainedNameList);

        private JList pointsGainedPointsList = new JList(dataPackage.getPlayers().toArray());
        private JScrollPane pointsGainedPointsPane = new JScrollPane(pointsGainedPointsList);

        public Panel() {
            //sets running to false when windows is closed to close all threads
            setLayout(null);//prevents any form of auto layout
            addMouseListener(this);//used to detect mouse actions
            addMouseMotionListener(this);//used to detect mouse dragging
            startMidi("bgmusic.mid");//starting music

            timerText.setBounds(50, 30, 75, 22);
            timerText.setFont(textFont);
            timerText.setEditable(false);
            timerText.setVisible(false);
            add(timerText);

            textField.setBounds(955, 578, 305, 22);
            textField.addKeyListener((KeyListener) new MKeyListener());
            add(textField);

            playerList.setCellRenderer(playerListRenderer());
            playerPane.setVerticalScrollBarPolicy(playerPane.VERTICAL_SCROLLBAR_NEVER);
            playerPane.setHorizontalScrollBarPolicy(playerPane.HORIZONTAL_SCROLLBAR_NEVER);
            add(playerPane);
            playerList.setFixedCellHeight(75);

            messageList.setCellRenderer(messageListRenderer());
            messagePane.setBounds(955, 50, 305, 525);
            messagePane.setHorizontalScrollBarPolicy(messagePane.HORIZONTAL_SCROLLBAR_NEVER);
            add(messagePane);

            pointsGainedNameList.setCellRenderer(pointsGainedNameListRenderer());
            pointsGainedNamePane.setVerticalScrollBarPolicy(playerPane.VERTICAL_SCROLLBAR_NEVER);
            pointsGainedNamePane.setHorizontalScrollBarPolicy(playerPane.HORIZONTAL_SCROLLBAR_NEVER);
            pointsGainedNameList.setFixedCellHeight(75);
            add(pointsGainedNamePane);

            pointsGainedPointsList.setCellRenderer(pointsGainedPointListRenderer());
            pointsGainedPointsList.setFixedCellHeight(75);
            add(pointsGainedPointsPane);
        }

        //I barley understand how this works so don't ask
        private ListCellRenderer<? super String> messageListRenderer() {
            return new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (c instanceof JLabel) {
                        JLabel label = (JLabel) c;
                        String message = messagesToRender.get(index);
                        label.setFont(textFont);
                        if (index % 2 != 0){
                            label.setBackground(new Color(235, 235, 235));
                        }
                        if (message.contains("~")) {
                            String[] messageParts = message.split("~");
                            label.setForeground(Color.decode(messageParts[0]));
                            label.setText(messageParts[1]);
                        } else {
                            label.setForeground(Color.black);
                            label.setText(messagesToRender.get(index));
                        }
                    }
                    return c;
                }
            };
        }

        private ImageIcon avatar = new ImageIcon("icon.png");
        private ListCellRenderer<? super Player> playerListRenderer(){
            return new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (c instanceof JLabel) {
                        JLabel label = (JLabel) c;
                        label.setFont(textFont);
                        label.setIcon(avatar);
                        if (index % 2 != 0){
                            label.setBackground(new Color(235, 235, 235));
                        }
                        if (dataPackage.getPlayers().get(index) == dataPackage.getMyPlayer()){
                            label.setText("<html>"+dataPackage.getPlayers().get(index).getName()+" (You)"+"<br>"+"Points: "+dataPackage.getPlayers().get(index).getScore()+"</html>");
                        }else {
                            label.setText("<html>" + dataPackage.getPlayers().get(index).getName() + "<br>" +"Points: "+ dataPackage.getPlayers().get(index).getScore() + "</html>");
                        }
                        label.setFont(textFont);
                        playerPane.setBounds(10, 50, 205, playerList.getFixedCellHeight()*dataPackage.getPlayers().size());
                    }
                    return c;
                }
            };
        }

        private ListCellRenderer<? super Player> pointsGainedNameListRenderer(){
            return new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (c instanceof JLabel) {
                        JLabel label = (JLabel) c;
                        label.setBackground(new Color(235, 235, 235));
                        label.setFont(textFont.deriveFont(35f));
                        label.setText(dataPackage.getPlayers().get(index).getName());
                        pointsGainedNamePane.setBorder(null);
                        pointsGainedNamePane.setBounds(400, 150, 205, playerList.getFixedCellHeight()*dataPackage.getPlayers().size());
                    }
                    return c;
                }
            };
        }

        private ListCellRenderer<? super Player> pointsGainedPointListRenderer(){
            return new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (c instanceof JLabel) {
                        JLabel label = (JLabel) c;
                        label.setFont(textFont.deriveFont(35f));
                        label.setBackground(new Color(235, 235, 235));
                        if (dataPackage.getPlayers().get(index).getPointsGainedLastRound() == 0){
                            label.setText(""+dataPackage.getPlayers().get(index).getPointsGainedLastRound());
                            label.setForeground(Color.red);
                        }else{
                            label.setText("+"+dataPackage.getPlayers().get(index).getPointsGainedLastRound());
                            label.setForeground(new Color(40, 200, 40));
                        }
                        label.setHorizontalAlignment(SwingConstants.CENTER);

                        pointsGainedPointsPane.setBorder(null);
                        pointsGainedPointsPane.setBounds(600, 150, 205, playerList.getFixedCellHeight()*dataPackage.getPlayers().size());
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
                //midiPlayer.start();
            } catch (MidiUnavailableException | InvalidMidiDataException | IOException e) {
                e.printStackTrace();
            }
        }

        private Image bgImage = new ImageIcon("bg4.jpg").getImage();
        //the canvas rectangle where the image is drawn
        private Rectangle canvasPanel = new Rectangle(225, 50, 725, 550);
        //loading the color palette image
        private Image colorPickerImage = new ImageIcon("Color picker.png").getImage();
        //the rectangle around the color picker (not actually displayed, but used to check if the user clicks in it)
        private Rectangle colorPickerPanel = new Rectangle(260, 610, colorPickerImage.getWidth(null), colorPickerImage.getHeight(null));
        //loads tool images and creates rectangle objects to check for collision
        private Image pencilImage = new ImageIcon("pencil.png").getImage();
        private Image eraserImage = new ImageIcon("eraser.png").getImage();
        private Image thickSelectImage1 = new ImageIcon("thick1.png").getImage();
        private Image thickSelectImage2 = new ImageIcon("thick2.png").getImage();
        private Image thickSelectImage3 = new ImageIcon("thick3.png").getImage();
        private Image thickSelectImage4 = new ImageIcon("thick4.png").getImage();
        private Rectangle pencilPanel = new Rectangle(610, 610, pencilImage.getWidth(null), pencilImage.getHeight(null));
        private Rectangle eraserPanel = new Rectangle(675, 610, eraserImage.getWidth(null), eraserImage.getHeight(null));
        private Rectangle thickSelectPanel1 = new Rectangle(740, 610, thickSelectImage1.getWidth(null), thickSelectImage1.getHeight(null));
        private Rectangle thickSelectPanel2 = new Rectangle(805, 610, thickSelectImage2.getWidth(null), thickSelectImage2.getHeight(null));
        private Rectangle thickSelectPanel3 = new Rectangle(870, 610, thickSelectImage3.getWidth(null), thickSelectImage3.getHeight(null));
        private Rectangle thickSelectPanel4 = new Rectangle(935, 610, thickSelectImage4.getWidth(null), thickSelectImage4.getHeight(null));

        private int previousMessagesToRenderSize = 0;

        //renders the GUI and calls any methods relates to the GUI
        public void paintComponent(Graphics g) {
            if (g != null) {
                Graphics2D g2 = (Graphics2D) g;
                g.drawImage(bgImage, 0,0, null);
                g.setColor(Color.white);
                g.fillRect((int) canvasPanel.getX(), (int) canvasPanel.getY(),//filling the canvas with white
                        (int) canvasPanel.getWidth(), (int) canvasPanel.getHeight());
                g2.drawRect((int)canvasPanel.getX(), (int)canvasPanel.getY(), (int)canvasPanel.getWidth(), (int)canvasPanel.getHeight());
                g.drawImage(colorPickerImage, (int) colorPickerPanel.getX(), (int) colorPickerPanel.getY(), null);
                //drawing the tool images
                g.drawImage(pencilImage, (int) pencilPanel.getX(), (int) pencilPanel.getY(), null);
                g.drawImage(eraserImage, (int) eraserPanel.getX(), (int) eraserPanel.getY(), null);
                g.drawImage(thickSelectImage1, (int) thickSelectPanel1.getX(), (int) thickSelectPanel1.getY(), null);
                g.drawImage(thickSelectImage2, (int) thickSelectPanel2.getX(), (int) thickSelectPanel2.getY(), null);
                g.drawImage(thickSelectImage3, (int) thickSelectPanel3.getX(), (int) thickSelectPanel3.getY(), null);
                g.drawImage(thickSelectImage4, (int) thickSelectPanel4.getX(), (int) thickSelectPanel4.getY(), null);

                //iterating through the drawing components and drawing each component onto the screen
                //basically drawing the image
                if (drawingComponents.size() > 0) {
                    for (DrawingComponent s : drawingComponents) {
                        g.setColor(s.getCol());
                        g.fillOval(s.getCx()-s.getStroke(), s.getCy()-s.getStroke(), s.getStroke()*2, s.getStroke()*2);
                    }
                }

                //TIMER
                if (dataPackage.getGameStatus().equals(DataPackage.ROUNDINPROGRESS)){
                    timerText.setVisible(true);
                    timerText.setText("Timer: " + dataPackage.getTimeRemaining());//updating the timer text using data package
                }else{
                    timerText.setVisible(false);
                }

                //MESSAGE LIST
                if (messagesToRender.size() > previousMessagesToRenderSize) {
                    messageList.setListData(messagesToRender.toArray());
                    previousMessagesToRenderSize = messagesToRender.size();
                    messagePaneScrollBar.setValue(messagePaneScrollBar.getMaximum());
                }
                messagePaneScrollBar.setValue(messagePaneScrollBar.getMaximum());//temp until fix

                //PLAYER LIST
                playerList.setListData(dataPackage.getPlayers().toArray());

                //POINTS GAINED PANEL

                if (dataPackage.getGameStatus().equals(DataPackage.BETWEENROUND)){
                    drawingComponents.clear();
                    g.setColor(new Color(235, 235, 235));
                    g.fillRect((int)canvasPanel.getX(), (int)canvasPanel.getY(), (int)canvasPanel.getWidth(), (int)canvasPanel.getHeight());
                    pointsGainedPointsList.setListData(dataPackage.getPlayers().toArray());
                    pointsGainedPointsPane.setVisible(true);

                    pointsGainedNameList.setListData(dataPackage.getPlayers().toArray());
                    pointsGainedNamePane.setVisible(true);
                }else{
                    pointsGainedPointsPane.setVisible(false);
                    pointsGainedNamePane.setVisible(false);
                }

            }
        }

        // ------------ MouseListener ------------------------------------------
        //I WILL ADD COMMENTS LATER BECAUSE THERE IS A LOT MORE CODE TO ADD HERE
        private int x1, y1, x2, y2;
        private int mouseDist, dx, dy;

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
            } else if (thickSelectPanel1.contains(x1, y1)) {
                DrawingComponent.setStroke(DrawingComponent.STROKE1);
            } else if (thickSelectPanel2.contains(x1, y1)) {
                DrawingComponent.setStroke(DrawingComponent.STROKE2);
            } else if (thickSelectPanel3.contains(x1, y1)) {
                DrawingComponent.setStroke(DrawingComponent.STROKE3);
            } else if (thickSelectPanel4.contains(x1, y1)) {
                DrawingComponent.setStroke(DrawingComponent.STROKE4);
            } else if (canvasPanel.contains(x1, y1) && dataPackage.getMyPlayer().isArtist()) {
                drawingComponents.add(new DrawingComponent(x1, y1));
            }
        }

        synchronized public void mouseDragged(MouseEvent e) {
            x2 = e.getX();
            y2 = e.getY();

//            if (canvasPanel.contains(x1, y1) && canvasPanel.contains(x2, y2) && dataPackage.getMyPlayer().isArtist()) {
            if (canvasPanel.contains(x1, y1) && canvasPanel.contains(x2, y2) && dataPackage.getMyPlayer().isArtist()) {
                if (DrawingComponent.getToolType().equals(DrawingComponent.PENCIL) || DrawingComponent.getToolType().equals(DrawingComponent.ERASER)) {
                    mouseDist = (int)(Math.hypot(x2-x1, y2-y1)+.5);
                    mouseDist = Math.max(mouseDist, 1);
                    for(int i = 0; i < mouseDist; i++){
                        dx = (int)(i*(x2-x1)/mouseDist+.5);
                        dy = (int)(i*(y2-y1)/mouseDist+.5);
                        drawingComponents.add(new DrawingComponent(x1+dx, y1+dy));
                    }
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

