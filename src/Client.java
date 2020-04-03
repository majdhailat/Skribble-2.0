import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.util.concurrent.TimeUnit;

public class Client extends JFrame {
    private volatile boolean running = true;
    private DataPackage dataPackage;
    private boolean canReceiveMessages = false;
    private String [] messageQueue = new String[17];
    private String inputMessage = null;

    public static void main(String[] args)  {
        new Client();
    }

    public Client(){
        String hostName = "localhost";
        int portNumber = 4445;
        try (Socket socket = new Socket(hostName, portNumber)){
            new ClientInputThread(socket).start();
            new ClientOutputThread(socket).start();
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {e.printStackTrace();}
            new Gui();
            while (running) {
                Thread.onSpinWait();
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
    public class ClientOutputThread extends Thread {
        private PrintWriter out;
        private boolean gotUserName = false;
        public ClientOutputThread(Socket socket) throws IOException {
            out = new PrintWriter(socket.getOutputStream(), true);
            addMessageToQueue("#FF0000~Enter a user name");
        }
        public void run() {
            while (running) {
                if (inputMessage != null){
                    if (gotUserName){
                        out.println(inputMessage);
                        addMessageToQueue("Me: "+ inputMessage);
                    }
                    else{
                        out.println("USERNAME " + inputMessage);
                        out.println("#00FFFF~"+ inputMessage +" has joined the game");
                        addMessageToQueue("#008000~Welcome to Skribble "+ inputMessage);
                        gotUserName = true;
                        canReceiveMessages = true;
                    }
                    inputMessage = null;
                }
            }
        }
    }

    public class ClientInputThread extends Thread {
        private ObjectInputStream objectInputStream;
        public ClientInputThread(Socket socket) throws IOException {
            objectInputStream = new ObjectInputStream(socket.getInputStream());
        }
        public void run() {
            while (running) {
                try {
                    dataPackage = (DataPackage) objectInputStream.readUnshared();
                    String message = dataPackage.getMessage();
                    if (message != null && canReceiveMessages) {
                        addMessageToQueue(message);
                    }
                } catch (IOException | ClassNotFoundException e) {e.printStackTrace();}
            }
        }
    }
    public void addMessageToQueue(String message){
        boolean messageWasAdded = false;
        for (int i = 0; i < messageQueue.length; i++) {
            if (messageQueue[i] == null) {
                messageQueue[i] = message;
                messageWasAdded = true;
                break;
            }
        }
        if (!messageWasAdded) {
            messageQueue[0] = null;
            for (int i = 1; i < messageQueue.length; i++) {
                messageQueue[i - 1] = messageQueue[i];
            }
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
            Timer myTimer = new Timer(10, new TickListener());     // trigger every 100 ms
            myTimer.start();
            panel = new Panel();
            add(panel);
            setResizable(false);
            setVisible(true);
        }
        class TickListener implements ActionListener {
            public void actionPerformed(ActionEvent evt) {
                if (panel != null && panel.ready) {
                    panel.repaint();
                }
            }
        }
    }
    public class Panel extends JPanel {
        public boolean ready = false;
        private int mouseX, mouseY;
        private Rectangle drawingPanel = new Rectangle(201, 64, 749, 562);
        private Rectangle chatPanel = new Rectangle(958, 64, 312, 562);


        public Panel(){
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    running = false;
                }
            });
            setBackground(Color.blue);
            setLayout(null);
            addMouseListener(new clickListener());
        }
        public void addNotify() {
            super.addNotify();
            requestFocus();
            ready = true;
        }

        public void paintComponent(Graphics g) {
            if (g != null) {
                g.setColor(Color.white);
                g.fillRect((int) drawingPanel.getX(), (int) drawingPanel.getY(), (int) drawingPanel.getWidth(), (int) drawingPanel.getHeight());
                g.setColor(new Color(237, 237, 237));
                g.fillRect((int) chatPanel.getX(), (int) chatPanel.getY(), (int) chatPanel.getWidth(), (int) chatPanel.getHeight());
                updateMessageTextAreas();
                updatePlayerTextAreas(g);
            }
        }

        private JTextArea[] messageTextAreas = new JTextArea[messageQueue.length];
        private JTextField textField = new JTextField();
        private boolean initializedMessageTextAreas = false;
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
                    int maxCharsPerLine = 42;
                    if (numOfChars > maxCharsPerLine) {
                        int numOfWraps = (int) Math.ceil((double) numOfChars / (double) maxCharsPerLine);
                        for (int j = 0; j < numOfWraps; j++) {
                            String line = message.substring((maxCharsPerLine * j), Math.min(((maxCharsPerLine * j) + maxCharsPerLine), message.length()));
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

        private JTextArea[] playerNameLabels = new JTextArea[8];
        private boolean initializedPlayerNameLabels = false;
        private Font nameLabelFont = new Font("Arial", Font.PLAIN,14);
        private Font myNameLabelFont = new Font("Arial", Font.BOLD,14);
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
            g.setColor(Color.white);
            for (int i = 0; i < dataPackage.getPlayers().size(); i++) {
                g.fillRect(10, 64 +  (75 * i), 183, 70);
                if (dataPackage.getPlayers().get(i) == dataPackage.getMyPlayer()){
                    playerNameLabels[i].setFont(myNameLabelFont);
                    playerNameLabels[i].setText(dataPackage.getPlayers().get(i).getName()+" (You)");
                }else{
                    playerNameLabels[i].setText(dataPackage.getPlayers().get(i).getName());
                    playerNameLabels[i].setFont(nameLabelFont);
                }
                playerNameLabels[i].setVisible(true);
                playerNameLabels[i].setForeground(Color.black);
            }
        }
        class clickListener implements MouseListener {
            // ------------ MouseListener ------------------------------------------
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseClicked(MouseEvent e) {}
            public void mousePressed(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
        }
        class MKeyListener extends KeyAdapter {
            public void keyPressed(KeyEvent event) {
                if(event.getKeyCode() == KeyEvent.VK_ENTER){
                    inputMessage = textField.getText();
                    textField.setText("");
                }
            }
        }
    }
}

