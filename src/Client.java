import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.awt.*;

public class Client extends JFrame {
    private static boolean canReceiveMessages = false;
    private static String [] messageQueue = new String[17];
    private static String inputMessage = null;
    private boolean majdSucksBalls = true;

    public static void main(String[] args)  {
        String hostName = "localhost";
        int portNumber = 4444;
        try (Socket socket = new Socket(hostName, portNumber)){
            new Gui();
            new ClientInputThread(socket).start();
            new ClientOutputThread(socket).start();
            while (true) {
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
    public static class ClientOutputThread extends Thread {
        private PrintWriter out;
        private boolean gotUserName = false;
        private String message;

        public ClientOutputThread(Socket socket) throws IOException {
            out = new PrintWriter(socket.getOutputStream(), true);
            addMessageToQueue("#FF0000~Enter a user name");
        }
        public void run() {
            while (true) {
                message = inputMessage;
                if (message != null){
                    if (gotUserName){
                        out.println(message);
                        addMessageToQueue("Me: "+ message);
                    }
                    else{
                        out.println("USERNAME " + message);
                        out.println("#00FFFF~"+ message +" has joined the game");
                        addMessageToQueue("#008000~Welcome to Skribble "+ message);
                        gotUserName = true;
                        canReceiveMessages = true;
                    }
                    inputMessage = null;
                }
            }
        }
    }

    public static class ClientInputThread extends Thread {
        private ObjectInputStream objectInputStream;
        private String message;
        public ClientInputThread(Socket socket) throws IOException {
            objectInputStream = new ObjectInputStream(socket.getInputStream());
        }
        public void run() {
            while (true) {
                try {
                    DataPackage dataPackage = (DataPackage) objectInputStream.readObject();
                    message = dataPackage.getMessage();
                    if (dataPackage.getPlayers().size() > 0){
                        System.out.println(dataPackage.getPlayers().get(0).getName());
                    }
                    if (message != null && canReceiveMessages) {
                        addMessageToQueue(message);
                    }
                } catch (IOException | ClassNotFoundException e) {e.printStackTrace();}
            }
        }
    }
    public static void addMessageToQueue(String message){
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
    public static class Gui extends JFrame {
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
                    panel.move();
                }
            }
        }
    }

    static class Panel extends JPanel {
        public boolean ready = false;
        private int mouseX, mouseY;
        private Rectangle drawingPanel = new Rectangle(201, 64, 749, 562);
        private Rectangle chatPanel = new Rectangle(958, 64, 312, 562);

        private JTextArea[] txtBoxes = new JTextArea[17];

        private JTextField textField = new JTextField();
        public Panel(){
            setBackground(Color.blue);
            setLayout(null);
            addMouseListener(new clickListener());
            textField.setBounds(964, 590, 294, 22);
            textField.addKeyListener((KeyListener) new MKeyListener());
            add(textField);
            for (int i = 0; i < txtBoxes.length; i++){
                JTextArea txtArea = new JTextArea();
                txtArea.setVisible(false);
                txtArea.setEditable(false);
                add(txtArea);
                txtBoxes[i] = txtArea;
            }
        }

        public void addNotify() {
            super.addNotify();
            requestFocus();
            ready = true;
        }

        int maxCharsPerLine = 42;
        public void paintComponent(Graphics g) {
            if (g != null) {
                g.setColor(Color.white);
                g.fillRect((int) drawingPanel.getX(), (int) drawingPanel.getY(), (int) drawingPanel.getWidth(), (int) drawingPanel.getHeight());
                g.setColor(new Color(237, 237, 237));
                g.fillRect((int) chatPanel.getX(), (int) chatPanel.getY(), (int) chatPanel.getWidth(), (int) chatPanel.getHeight());

                for (int i = 0; i < 17; i++) {
                    String message = messageQueue[i];
                    Color col = Color.black;
                    if (messageQueue[i] != null){
                        if (messageQueue[i].contains("~")){
                            String [] messageParts = messageQueue[i].split("~");
                            col = Color.decode(messageParts[0]);
                            message = messageParts[1];
                        }
                        int numOfChars = message.length();
                        if (numOfChars > maxCharsPerLine ) {
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
                        txtBoxes[i].setVisible(true);
                        txtBoxes[i].setText(message);
                        txtBoxes[i].setForeground(col);
                        txtBoxes[i].setBounds(964, 73+(30*i), 300, 20);
                    }
                }
            }
        }

        public void move(){
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

