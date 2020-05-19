import javax.imageio.ImageIO;
import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class Panel extends JPanel implements MouseListener, MouseMotionListener {
    public boolean ready = false;

    private boolean loadedAssets;
    private DataPackage dataPackage;
    private ArrayList<DrawingComponent> drawingComponents; //this arrayList contains all DrawingComponents from the round
    private ArrayList<String> messagesToRender;
    private int previousMessagesToRenderSize = 0;

    private List<DrawingComponent> drawingComponentsToDraw;

    Font textFont;
    private ImageIcon avatar;
    private Image bgImage, OGCanvasImage, canvasImage, colorPickerImage, pencilImage, pencilSelectedImage,
            eraserImage, eraserSelectedImage, thick1Image, thick2Image, thick3Image, thick4Image, alarmImage,
            letterPlaceHolderImage, thick1SelectedImage, thick2SelectedImage, thick3SelectedImage, thick4SelectedImage,
            speakerImage, speakerMuteImage;
    BufferedImage bufferedColorPickerImage;
    private Rectangle canvasPanel, colorPickerPanel, pencilPanel, eraserPanel, thickSelectPanel1, thickSelectPanel2,
            thickSelectPanel3, thickSelectPanel4, playPausePanel;
    private JTextField textField;
    private JList messageList, playerList;
    private JScrollPane messagePane, playerPane;
    private JScrollBar messagePaneScrollBar;
    Image screenshot = null;

    private Client client;
    public Panel(Client client) throws IOException{
        this.client = client;
        updateFromClient();

        setLayout(null);//prevents any form of auto layout
        addMouseListener(this);//used to detect mouse actions
        addMouseMotionListener(this);//used to detect mouse dragging
        startMidi("assets/bgmusic.mid");//starting music

        loadAssets();
        loadRects();
        loadGuiComponents();
        loadedAssets = true;
    }

    public void addNotify() {
        super.addNotify();
        requestFocus();
        ready = true;
    }

    private int chunks = 0;
    public void updateFromClient(){

        drawingComponents = client.getDrawingComponentsArrayList();
        drawingComponentsToDraw = drawingComponents.subList(chunks * 300, drawingComponents.size());
        System.out.println("CHUCK SIZE    :" + drawingComponents.subList(chunks * 300, drawingComponents.size()).size());
        dataPackage = client.getDataPackage();
        messagesToRender = client.getMessagesToRender();
        if (loadedAssets) {
            if (drawingComponentsToDraw.size() > 300){
                drawingComponentsToDraw.clear();
                chunks += 1;
                screenshot = null;
                screenshot = ScreenImage.createImage(this, new Rectangle((int)canvasPanel.getX(), (int)canvasPanel.getY(), (int)canvasPanel.getWidth(), (int)canvasPanel.getHeight()));

            }


            if (messagesToRender.size() > previousMessagesToRenderSize) {
                messageList.setListData(messagesToRender.toArray());
                previousMessagesToRenderSize = messagesToRender.size();
                messagePaneScrollBar.setValue(messagePaneScrollBar.getMaximum());
            }
            messagePaneScrollBar.setValue(messagePaneScrollBar.getMaximum());//temp until fix
        }
    }

    //takes music file path, loads music and plays it in loop
    Sequencer midiPlayer;{
        try {
            midiPlayer = MidiSystem.getSequencer();
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        }
    }

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

    //renders the GUI and calls any methods relates to the GUI
    public void paintComponent(Graphics g) {

        if (g != null && loadedAssets) {
            g.drawImage(canvasImage, (int) canvasPanel.getX(), (int) canvasPanel.getY(), null);
            g.drawImage(bgImage, 0,0, null);
            drawUI(g);
            g.drawImage(screenshot, (int)canvasPanel.getX(), (int) canvasPanel.getY(), null);
            drawDrawingComponents(g);

            if (dataPackage.getMyPlayer().isArtist()) {
                drawArtistToolsPane(g);
            }
        }
    }

    public void drawDrawingComponents(Graphics g){
        updateFromClient();
        long start = System.currentTimeMillis();

        for (DrawingComponent s : drawingComponentsToDraw){
            g.setColor(s.getCol());
            g.fillOval(s.getCx()-s.getStroke(), s.getCy()-s.getStroke(), s.getStroke()*2, s.getStroke()*2);
        }
        long end = System.currentTimeMillis();
    }

    public void drawArtistToolsPane(Graphics g){
        g.drawImage(colorPickerImage, (int) colorPickerPanel.getX(), (int) colorPickerPanel.getY(), null);
        if (DrawingComponent.getToolType().equals(DrawingComponent.PENCIL)){
            g.drawImage(pencilSelectedImage, (int) pencilPanel.getX(), (int) pencilPanel.getY(), null);
        }else {
            g.drawImage(pencilImage, (int) pencilPanel.getX(), (int) pencilPanel.getY(), null);
        }
        if (DrawingComponent.getToolType().equals(DrawingComponent.ERASER)){
            g.drawImage(eraserSelectedImage, (int) eraserPanel.getX(), (int) eraserPanel.getY(), null);
        }else {
            g.drawImage(eraserImage, (int) eraserPanel.getX(), (int) eraserPanel.getY(), null);
        }

        g.drawImage(thick1Image, (int) thickSelectPanel1.getX(), (int) thickSelectPanel1.getY(), null);
        g.drawImage(thick2Image, (int) thickSelectPanel2.getX(), (int) thickSelectPanel2.getY(), null);
        g.drawImage(thick3Image, (int) thickSelectPanel3.getX(), (int) thickSelectPanel3.getY(), null);
        g.drawImage(thick4Image, (int) thickSelectPanel4.getX(), (int) thickSelectPanel4.getY(), null);
        int strokeSize = DrawingComponent.getCurrentStrokeSize();
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
        g2.setColor(DrawingComponent.getCurrentColor());
        g2.setStroke(new BasicStroke(3));
        g2.drawRect((int)colorPickerPanel.getX() - 2, (int)colorPickerPanel.getY() - 2, colorPickerImage.getWidth(null) + 4, colorPickerImage.getHeight(null) + 4);
    }

    public void drawUI(Graphics g){
        g.setColor(Color.black);
        g.setFont(textFont.deriveFont(20f));
        String status = dataPackage.getGameStatus();
        if (midiPlayer.isRunning()){
            g.drawImage(speakerImage, (int)playPausePanel.getX(), (int)playPausePanel.getY(), null);
        }else{
            g.drawImage(speakerMuteImage, (int)playPausePanel.getX(), (int)playPausePanel.getY(), null);
        }

        playerList.setListData(dataPackage.getPlayers().toArray());
        playerList.setCellRenderer(PanelExtension.playerListRenderer(avatar, textFont, dataPackage.getMyPlayer()));
        playerPane.setBounds(10, 50, 205, playerList.getFixedCellHeight()*dataPackage.getPlayers().size());

        if (status.equals(DataPackage.ROUNDINPROGRESS)){
            g.drawImage(alarmImage, 14, 5, null);
            g.drawString(""+dataPackage.getTimeRemaining(), 22, 32);

            if (!dataPackage.getMyPlayer().isArtist()){
                int wordLen = dataPackage.getMagicWord().length();
                for (int i = 0; i < wordLen; i++) {
                    if (dataPackage.getMagicWord().charAt(i) != ' ') {
                        g.drawImage(letterPlaceHolderImage, (640 - ((wordLen * 40 + ((wordLen - 1) * 20)) / 2)) + ((i - 1) * 60), 30, null);
                    }
                }
            }
            else if (dataPackage.getMyPlayer().isArtist()){

            }
        }

        else if (status.equals(DataPackage.BETWEENROUND) || status.equals(DataPackage.WAITINGTOSTART)){
            drawingComponents.clear();
        }

        if (status.equals(DataPackage.BETWEENROUND) || status.equals(DataPackage.ROUNDINPROGRESS)){
            g.drawString("Round "+(dataPackage.getTotalNumOfRounds() - dataPackage.getRoundsLeft() + 1) +" of "+dataPackage.getTotalNumOfRounds(), 65, 32);
        }
    }

    // ------------ MouseListener ------------------------------------------
    //I WILL ADD COMMENTS LATER BECAUSE THERE IS A LOT MORE CODE TO ADD HERE
    private int x1, y1, x2, y2;
    private int mouseDist, dx, dy;

    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseClicked(MouseEvent e) {}

    public void mousePressed(MouseEvent e) {
        x1 = e.getX();
        y1 = e.getY();
        if (colorPickerPanel.contains(x1, y1)) {
            Color c = new Color(bufferedColorPickerImage.getRGB((int) (x1 - colorPickerPanel.getX()), (int) (y1 - colorPickerPanel.getY())));
            DrawingComponent.setColor(c);
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
        }else if (playPausePanel.contains(x1, y1)){
            if (midiPlayer.isRunning()){
                midiPlayer.stop();
            }else{
                midiPlayer.start();
            }
        }
    }

    synchronized public void mouseDragged(MouseEvent e) {
        x2 = e.getX();
        y2 = e.getY();

        if (canvasPanel.contains(x1, y1) && dataPackage.getMyPlayer().isArtist()) {
            if (DrawingComponent.getToolType().equals(DrawingComponent.PENCIL) || DrawingComponent.getToolType().equals(DrawingComponent.ERASER)) {
                mouseDist = (int)(Math.hypot(x2-x1, y2-y1)+.5);
                mouseDist = Math.max(mouseDist, 1);
                for (int i = 0; i < mouseDist; i += 2) {
                    dx = (int) (i * (x2 - x1) / mouseDist + .5);
                    dy = (int) (i * (y2 - y1) / mouseDist + .5);
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

    public void loadAssets() throws IOException {
        bufferedColorPickerImage = ImageIO.read(new File("image assets/Color picker.png"));
        avatar = new ImageIcon("image assets/icon.png");
//      bgImage = new ImageIcon("image assets/bg/bg"+Client.randint(1,10)+".jpg").getImage();
        bgImage = new ImageIcon("image assets/bg/bgTest.png").getImage();
        OGCanvasImage = new ImageIcon("image assets/canvas.png").getImage();
        canvasImage = OGCanvasImage;
        colorPickerImage = new ImageIcon("image assets/Color picker.png").getImage();
        pencilImage = new ImageIcon("image assets/pencil.png").getImage();
        eraserImage = new ImageIcon("image assets/eraser.png").getImage();
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

    public void loadRects(){
        canvasPanel = new Rectangle(225, 50, OGCanvasImage.getWidth(null), OGCanvasImage.getHeight(null));
        colorPickerPanel = new Rectangle(225, 610, colorPickerImage.getWidth(null), colorPickerImage.getHeight(null));
        pencilPanel = new Rectangle(575, 610, pencilImage.getWidth(null), pencilImage.getHeight(null));
        eraserPanel = new Rectangle(630, 610, eraserImage.getWidth(null), eraserImage.getHeight(null));
        thickSelectPanel1 = new Rectangle(705, 610, thick1Image.getWidth(null), thick1Image.getHeight(null));
        thickSelectPanel2 = new Rectangle(770, 610, thick2Image.getWidth(null), thick2Image.getHeight(null));
        thickSelectPanel3 = new Rectangle(835, 610, thick3Image.getWidth(null), thick3Image.getHeight(null));
        thickSelectPanel4 = new Rectangle(900, 610, thick4Image.getWidth(null), thick4Image.getHeight(null));
        playPausePanel = new Rectangle(1200, -3, speakerImage.getWidth(null), speakerImage.getHeight(null));
    }

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

        playerList.setCellRenderer(PanelExtension.playerListRenderer(avatar, textFont, dataPackage.getMyPlayer()));
        playerPane.setVerticalScrollBarPolicy(playerPane.VERTICAL_SCROLLBAR_NEVER);
        playerPane.setHorizontalScrollBarPolicy(playerPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(playerPane);
        playerList.setFixedCellHeight(75);

        messageList.setCellRenderer(PanelExtension.messageListRenderer(textFont));
        messagePane.setBounds(955, 50, 305, 525);
        messagePane.setHorizontalScrollBarPolicy(messagePane.HORIZONTAL_SCROLLBAR_NEVER);
        add(messagePane);
    }
}
