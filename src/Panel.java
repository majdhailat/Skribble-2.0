//imports
import javax.imageio.ImageIO;
import javax.sound.midi.*;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

//DIDN'T COMMENT HANDLE DRAWING COMPONENTS OR ARTIST TOOL BAR OR INIT METHODS
public class Panel extends JPanel implements MouseListener, MouseMotionListener {
    public boolean ready = false;
    public boolean legacyDCModeEnabled = true;//toggles between drawing component handling systems

    private Client client;
    private DataPackage dataPackage;//all the info needed by the panel from the client
    private ArrayList<DrawingComponent> drawingComponents; //this arrayList contains all DrawingComponents from the round
    private ArrayList<String> messagesToRender;//list of messages that have not been rendered yet
    private int previousMessagesToRenderSize = 0;

    //all assets
    private boolean loadedAssets;//if all assets were loaded
    private Font textFont;
    private Image bgImage, OGCanvasImage, canvasImage, colorPickerImage, pencilImage, pencilSelectedImage,
            eraserImage, eraserSelectedImage, clearImage, thick1Image, thick2Image, thick3Image, thick4Image, alarmImage,
            letterPlaceHolderImage, thick1SelectedImage, thick2SelectedImage, thick3SelectedImage, thick4SelectedImage,
            speakerImage, speakerMuteImage, screenshot = null;
    private BufferedImage bufferedColorPickerImage;
    private Rectangle canvasPanel, colorPickerPanel, pencilPanel, eraserPanel, clearPanel, thickSelectPanel1, thickSelectPanel2,
            thickSelectPanel3, thickSelectPanel4, playPausePanel;

    private JTextField textField;//the text field where the user can type
    private JList messageList, playerList;//the left and right list panes
    private JScrollPane messagePane, playerPane;//the scroll panes on which the lists are contained
    private JScrollBar messagePaneScrollBar;//scroll bar for message scroll pane

    private int initialScreenPosX, initialScreenPosY;//the pos of the panel on screen when the game is first loaded
    private boolean isMouseClicked = false;//if the mouse is being clicked

    public Panel(Client client) throws IOException, LineUnavailableException {
        this.client = client;
        updateFromClient();

        setLayout(null);//prevents any form of auto layout
        addMouseListener(this);//used to detect mouse actions
        addMouseMotionListener(this);//used to detect mouse dragging
        playSound();

        loadAssets();
        loadRects();
        loadGuiComponents();
        loadedAssets = true;
    }

    /*
    Fetches the data package and messages from the client
    Gets any info from the data package that is needed
     */
    public void updateFromClient(){
        dataPackage = client.getDataPackage();//getting data package
        messagesToRender = client.getMessagesToRender();//getting messages
        if (loadedAssets && messagesToRender.size() > previousMessagesToRenderSize) {//checking for any new messages
            playerList.setListData(dataPackage.getPlayers().toArray());
            messageList.setListData(messagesToRender.toArray());//setting the data for the messages list to the updated array
            previousMessagesToRenderSize = messagesToRender.size();//resetting size

        }
        if (loadedAssets){
            messagePaneScrollBar.setValue(messagePaneScrollBar.getMaximum());//temp until fix
        }
    }

    /*
    Calls the draw ui method and the drawing component handler which renders the drawing
     */
    public void paintComponent(Graphics g) {
        if (g != null && loadedAssets) {
            drawUI(g);
            handleDrawingComponents(g);
        }
    }

    /*
    Draws all UI elements
     */
    public void drawUI(Graphics g){
        playerPane.setBounds(10, 50, 205, playerList.getFixedCellHeight()*dataPackage.getPlayers().size());

        //BACKGROUND ~~~
        g.drawImage(bgImage, 0,0, null);//drawing background

        //ARTIST TOOL BAR ~~~
        if (dataPackage.getMyPlayer().isArtist()){//checking if artist
            drawArtistToolsPane(g);//drawing tools pane
        }


        //MUSIC SPEAKER ICON ~~~
        if (clip.isActive()){//checking if music is playing and drawing icon accordingly
            g.drawImage(speakerImage, (int)playPausePanel.getX(), (int)playPausePanel.getY(), null);
        }else{
            g.drawImage(speakerMuteImage, (int)playPausePanel.getX(), (int)playPausePanel.getY(), null);
        }

        g.setColor(Color.black);
        g.setFont(textFont.deriveFont(20f));
        String status = dataPackage.getGameStatus();//getting game status

        if (status.equals(DataPackage.WAITINGTOSTART)){
            g.drawImage(canvasImage, (int)canvasPanel.getX(), (int)canvasPanel.getY(), null);
            g.drawString("Waiting for players", 520, 30);
        }
        if (status.equals(DataPackage.ROUNDINPROGRESS)) {
            //TIMER ~~~
            g.drawImage(alarmImage, 14, 5, null);//drawing alarm image
            g.drawString("" + dataPackage.getTimeRemaining(), 22, 32);//drawing time remaining text

            //WORD/ LETTER PLACEHOLDERS ~~~
            if (!dataPackage.getMyPlayer().isArtist()) {//not artist
                int wordLen = dataPackage.getMagicWord().length();
                for (int i = 0; i < wordLen; i++) {
                    if (dataPackage.getMagicWord().charAt(i) != ' ') {//skipping spaces
                        g.drawImage(letterPlaceHolderImage, (640 - ((wordLen * 40 + ((wordLen - 1) * 12)) / 2)) + ((i - 1) * 60), 30, null);
                    }
                }
            }else{//artist
                g.drawString(""+dataPackage.getMagicWord(), 640 - ((dataPackage.getMagicWord().length() / 2) * 25), 30);
            }
        }

        if (status.equals(DataPackage.BETWEENROUND) || status.equals(DataPackage.ROUNDINPROGRESS)){
            //ROUND TEXT ~~~
            g.drawString("Round "+(dataPackage.getTotalNumOfRounds() - dataPackage.getRoundsLeft() + 1) +" of "+dataPackage.getTotalNumOfRounds(), 65, 32);
        }

        if (status.equals(DataPackage.BETWEENROUND)){
            g.setFont(textFont.deriveFont(35f));
            //POINTS SUMMARY ~~~
            //canvasPanel = new Rectangle(225, 50, OGCanvasImage.getWidth(null), OGCanvasImage.getHeight(null));
            g.drawImage(canvasImage, (int)canvasPanel.getX(), (int)canvasPanel.getY(), null);
            for (int i = 0; i < dataPackage.getPlayers().size(); i ++){
                Player p = dataPackage.getPlayers().get(i);
                g.drawString(""+p.getName()+":",400, 105 + (i * 55));
                g.drawString(""+p.getPointsGainedLastRound(), 700, 105 + (i * 55));
            }
        }
    }

    /*
    Draws the image on canvas
     */
    private boolean finishedIterating = true;
    boolean gotInitialPos = false;
    private int previousComponentSize;
    public void handleDrawingComponents(Graphics g) {
        drawingComponents = client.getDrawingComponentsArrayList();
        if (dataPackage.getGameStatus().equals(DataPackage.ROUNDINPROGRESS)) {
            g.drawImage(canvasImage, (int) canvasPanel.getX(), (int) canvasPanel.getY(), null);
            if (!legacyDCModeEnabled) {
                if (!gotInitialPos) {
                    Point location = this.getLocationOnScreen();
                    initialScreenPosX = location.x;
                    initialScreenPosY = location.y;
                    gotInitialPos = true;
                }
                int horizontalDisplacement, verticalDisplacement;
                if (finishedIterating) {
                    finishedIterating = false;
                    if (drawingComponents.size() > 0) {
                        g.drawImage(screenshot, (int) canvasPanel.getX(), (int) canvasPanel.getY(), null);
                        for (DrawingComponent s : drawingComponents) {
                            g.setColor(s.getCol());
                            g.fillOval(s.getCx() - s.getStroke(), s.getCy() - s.getStroke(), s.getStroke() * 2, s.getStroke() * 2);
                        }
                        if ((drawingComponents.size() >= 2000 && !isMouseClicked) || drawingComponents.size() < previousComponentSize) {
                            Point currentLocation = this.getLocationOnScreen();
                            horizontalDisplacement = currentLocation.x - initialScreenPosX;
                            verticalDisplacement = currentLocation.y - initialScreenPosY;

                            try {
                                screenshot = ScreenImage.createImage(new Rectangle((int) canvasPanel.getX() + 8 + horizontalDisplacement, (int) canvasPanel.getY() + 31 + verticalDisplacement, (int) canvasPanel.getWidth(), (int) canvasPanel.getHeight()));
                            } catch (AWTException e) {
                                e.printStackTrace();
                            }
                            drawingComponents.clear();
                        }
                    }
                    previousComponentSize = drawingComponents.size();
                    finishedIterating = true;
                }
            } else {
                for (DrawingComponent s : drawingComponents) {
                    g.setColor(s.getCol());
                    g.fillOval(s.getCx() - s.getStroke(), s.getCy() - s.getStroke(), s.getStroke() * 2, s.getStroke() * 2);
                }
            }
        }
        else{
            if (drawingComponents.size() > 0) {
                drawingComponents.clear();
            }
        }
    }

    /*
    Draws the artists tool bar
     */
    public void drawArtistToolsPane(Graphics g){
        g.drawImage(colorPickerImage, (int) colorPickerPanel.getX(), (int) colorPickerPanel.getY(), null);
        if (DrawingComponent.getSelectedToolType().equals(DrawingComponent.PENCIL)){
            g.drawImage(pencilSelectedImage, (int) pencilPanel.getX(), (int) pencilPanel.getY(), null);
        }else {
            g.drawImage(pencilImage, (int) pencilPanel.getX(), (int) pencilPanel.getY(), null);
        }
        if (DrawingComponent.getSelectedToolType().equals(DrawingComponent.ERASER)){
            g.drawImage(eraserSelectedImage, (int) eraserPanel.getX(), (int) eraserPanel.getY(), null);
        }else {
            g.drawImage(eraserImage, (int) eraserPanel.getX(), (int) eraserPanel.getY(), null);
        }

        g.drawImage(clearImage, (int) clearPanel.getX(), (int) clearPanel.getY(), null);
        g.drawImage(thick1Image, (int) thickSelectPanel1.getX(), (int) thickSelectPanel1.getY(), null);
        g.drawImage(thick2Image, (int) thickSelectPanel2.getX(), (int) thickSelectPanel2.getY(), null);
        g.drawImage(thick3Image, (int) thickSelectPanel3.getX(), (int) thickSelectPanel3.getY(), null);
        g.drawImage(thick4Image, (int) thickSelectPanel4.getX(), (int) thickSelectPanel4.getY(), null);
        int strokeSize = DrawingComponent.getSelectedStrokeSize();
        if (strokeSize == DrawingComponent.STROKE1){
            g.drawImage(thick1SelectedImage, (int) thickSelectPanel1.getX(), (int) thickSelectPanel1.getY(), null);
        }
        else if(strokeSize == DrawingComponent.STROKE2){
            g.drawImage(thick2SelectedImage, (int) thickSelectPanel2.getX(), (int) thickSelectPanel2.getY(), null);
        }
        else if(strokeSize == DrawingComponent.STROKE3){
            g.drawImage(thick3SelectedImage, (int) thickSelectPanel3.getX(), (int) thickSelectPanel3.getY(), null);
        }else{
            g.drawImage(thick4SelectedImage, (int) thickSelectPanel4.getX(), (int) thickSelectPanel4.getY(), null);
        }
        Graphics2D g2 = (Graphics2D)g;
        g2.setColor(DrawingComponent.getSelectedColour());
        g2.setStroke(new BasicStroke(3));
        g2.drawRect((int)colorPickerPanel.getX() - 2, (int)colorPickerPanel.getY() - 2, colorPickerImage.getWidth(null) + 4, colorPickerImage.getHeight(null) + 4);
    }

    // ------------ MouseListener ------------------------------------------

    private int x1, y1;

    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {
        isMouseClicked = false;
        isMouseClicked = false;
    }
    public void mouseClicked(MouseEvent e) {}


    public void mousePressed(MouseEvent e) {
        isMouseClicked = true;
        x1 = e.getX();
        y1 = e.getY();
        if (colorPickerPanel.contains(x1, y1)) {
            Color c = new Color(bufferedColorPickerImage.getRGB((int) (x1 - colorPickerPanel.getX()), (int) (y1 - colorPickerPanel.getY())));
            DrawingComponent.setSelectedColour(c);
        } else if (pencilPanel.contains(x1, y1)) {
            DrawingComponent.setSelectedToolType("PENCIL");
        } else if (eraserPanel.contains(x1, y1)) {
            DrawingComponent.setSelectedToolType("ERASER");
        } else if(clearPanel.contains(x1, y1)){
            drawingComponents.clear();
            screenshot = canvasImage;
        } else if (thickSelectPanel1.contains(x1, y1)) {
            DrawingComponent.setSelectedStrokeSize(DrawingComponent.STROKE1);
        } else if (thickSelectPanel2.contains(x1, y1)) {
            DrawingComponent.setSelectedStrokeSize(DrawingComponent.STROKE2);
        } else if (thickSelectPanel3.contains(x1, y1)) {
            DrawingComponent.setSelectedStrokeSize(DrawingComponent.STROKE3);
        } else if (thickSelectPanel4.contains(x1, y1)) {
            DrawingComponent.setSelectedStrokeSize(DrawingComponent.STROKE4);
        } else if (canvasPanel.contains(x1, y1) && dataPackage.getMyPlayer().isArtist()) {
            drawingComponents.add(new DrawingComponent(x1, y1));
        }else if (playPausePanel.contains(x1, y1)){
            if (clip.isActive()){
                clip.stop();
            }else{
                clip.start();
            }
        }
    }

    synchronized public void mouseDragged(MouseEvent e) {
        int x2 = e.getX();
        int y2 = e.getY();

        if (canvasPanel.contains(x1, y1) && dataPackage.getMyPlayer().isArtist()) {
            if (DrawingComponent.getSelectedToolType().equals(DrawingComponent.PENCIL) || DrawingComponent.getSelectedToolType().equals(DrawingComponent.ERASER)) {
                int mouseDist = (int) (Math.hypot(x2 - x1, y2 - y1) + .5);
                mouseDist = Math.max(mouseDist, 1);
                for (int i = 0; i < mouseDist; i += 2) {
                    int dx = (int) (i * (x2 - x1) / mouseDist + .5);
                    int dy = (int) (i * (y2 - y1) / mouseDist + .5);
                    if (!(x1 + dx < canvasPanel.getX()) && !(x1 + dx > canvasPanel.getX() + canvasPanel.getWidth()) &&
                            !(y1 + dy < canvasPanel.getY()) && !(y1 + dy > canvasPanel.getY() + canvasPanel.getHeight())) {
                        drawingComponents.add(new DrawingComponent(x1 + dx, y1 + dy));
                    }
                }

            }
        }
        x1 = x2;
        y1 = y2;
    }

    public void mouseMoved(MouseEvent e) {}

    class MKeyListener extends KeyAdapter {
        public void keyPressed(KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.VK_ENTER) {
                if (!textField.getText().equals("")) {
                    client.updateUsersTextMessage(textField.getText() );
                    textField.setText("");
                }
            }
        }
    }

    // ------------ SETUP ------------------------------------------

    /*
    Loads all assets
     */
    public void loadAssets() throws IOException {
        PanelExtension.loadIcons();
        bufferedColorPickerImage = ImageIO.read(new File("image assets/Color picker.png"));
        bgImage = new ImageIcon("image assets/bg/bg"+Client.randint(1, 10)+".jpg").getImage();
        OGCanvasImage = new ImageIcon("image assets/canvas.png").getImage();
        canvasImage = OGCanvasImage;
        colorPickerImage = new ImageIcon("image assets/Color picker.png").getImage();
        pencilImage = new ImageIcon("image assets/pencil.png").getImage();
        eraserImage = new ImageIcon("image assets/eraser.png").getImage();
        clearImage = new ImageIcon("image assets/clear.png").getImage();
        thick1Image = new ImageIcon("image assets/thick1.png").getImage();
        thick2Image = new ImageIcon("image assets/thick2.png").getImage();
        thick3Image = new ImageIcon("image assets/thick3.png").getImage();
        thick4Image = new ImageIcon("image assets/thick4.png").getImage();
        thick1SelectedImage = new ImageIcon("image assets/thick1 selected.png").getImage();
        thick2SelectedImage = new ImageIcon("image assets/thick2 selected.png").getImage();
        thick3SelectedImage = new ImageIcon("image assets/thick3 selected.png").getImage();
        thick4SelectedImage = new ImageIcon("image assets/thick4 selected.png").getImage();
        alarmImage = new ImageIcon("image assets/alarm.png").getImage();
        letterPlaceHolderImage = new ImageIcon("image assets/letter place holder.png").getImage();
        pencilSelectedImage = new ImageIcon("image assets/pencil selected.png").getImage();
        eraserSelectedImage = new ImageIcon("image assets/eraser selected.png").getImage();
        speakerImage = new ImageIcon("image assets/speaker.png").getImage();
        speakerMuteImage = new ImageIcon("image assets/speaker mute.png").getImage();
        try {
            textFont = Font.createFont(Font.TRUETYPE_FONT, new File("assets/tipper.otf")).deriveFont(15.5f);
        } catch (FontFormatException | IOException e) {e.printStackTrace();}
    }

    /*
    Loads all rectangles
     */
    public void loadRects(){
        canvasPanel = new Rectangle(225, 50, OGCanvasImage.getWidth(null), OGCanvasImage.getHeight(null));
        colorPickerPanel = new Rectangle(225, 610, colorPickerImage.getWidth(null), colorPickerImage.getHeight(null));
        pencilPanel = new Rectangle(575, 610, pencilImage.getWidth(null), pencilImage.getHeight(null));
        eraserPanel = new Rectangle(630, 610, eraserImage.getWidth(null), eraserImage.getHeight(null));
        clearPanel = new Rectangle(695, 610, clearImage.getWidth(null), clearImage.getHeight(null));
        thickSelectPanel1 = new Rectangle(760, 610, thick1Image.getWidth(null), thick1Image.getHeight(null));
        thickSelectPanel2 = new Rectangle(825, 610, thick2Image.getWidth(null), thick2Image.getHeight(null));
        thickSelectPanel3 = new Rectangle(890, 610, thick3Image.getWidth(null), thick3Image.getHeight(null));
        thickSelectPanel4 = new Rectangle(955, 610, thick4Image.getWidth(null), thick4Image.getHeight(null));
        playPausePanel = new Rectangle(1200, -3, speakerImage.getWidth(null), speakerImage.getHeight(null));
    }

    /*
    Loads all UI components
     */
    private void loadGuiComponents(){
        textField = new JTextField();//the box in which the user can type their message
        messageList = new JList(messagesToRender.toArray());
        messagePane = new JScrollPane(messageList);
        messagePaneScrollBar = messagePane.getVerticalScrollBar();
        playerList = new JList(dataPackage.getPlayers().toArray());
        playerPane = new JScrollPane(playerList);

        textField.setBounds(955, 578, 305, 22);
        textField.addKeyListener(new MKeyListener());
        add(textField);

        playerList.setCellRenderer(PanelExtension.playerListRenderer(textFont, dataPackage.getMyPlayer()));
        playerPane.setVerticalScrollBarPolicy(playerPane.VERTICAL_SCROLLBAR_NEVER);
        playerPane.setHorizontalScrollBarPolicy(playerPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(playerPane);
        playerList.setFixedCellHeight(75);

        messageList.setCellRenderer(PanelExtension.messageListRenderer(textFont));
        messagePane.setBounds(955, 50, 305, 520);
        messagePane.setHorizontalScrollBarPolicy(messagePane.HORIZONTAL_SCROLLBAR_NEVER);
        add(messagePane);
    }

    public void addNotify() {
        super.addNotify();
        requestFocus();
        ready = true;
    }

    /*
    Sequencer midiPlayer;{
        try {
            midiPlayer = MidiSystem.getSequencer();
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        }
    }

     */


    /*
    Starts music player

    public void startMidi(String midFilename) {
        try {
            File midiFile = new File(midFilename);
            Sequence song = MidiSystem.getSequence(midiFile);
            midiPlayer.open();
            midiPlayer.setSequence(song);
            midiPlayer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            midiPlayer.start();
        } catch (MidiUnavailableException | InvalidMidiDataException | IOException e) {e.printStackTrace();}
    }
    */

    public Clip clip = AudioSystem.getClip();
    public void playSound() {
        clip.loop(Clip.LOOP_CONTINUOUSLY);
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File("assets/lit(var).wav").getAbsoluteFile());
            clip.open(audioInputStream);
            clip.start();
        } catch(Exception ex) {
            System.out.println("Error with playing sound.");
            ex.printStackTrace();
        }
    }
}
