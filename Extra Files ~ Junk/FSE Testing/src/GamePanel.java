/* GamePanel.java
 * 
 * This is Game4 with a menu added. 
 * 
 * This method is simply using a variable to control the screen. I think keeping the 
 * game and menu on separate JFrames is better, but I imagine some will prefer this method.
 *
 * This example also shows making your own simple buttons rather than using swing buttons.
 **/

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.MouseInfo;

class GamePanel extends JPanel implements KeyListener, MouseListener{
	private boolean [] keys;
	private int screen, boxx, boxy;
	public static final int MENU=1, GAME=2, INSTRUCTIONS=3;
	private Point mouse;
	private Image buttonUp, buttonDown; 
	private boolean ready=false;
	private Rectangle menuInstructions, menuPlay;
	private MenuEg mainFrame;
		
	public GamePanel(MenuEg m){
		mainFrame = m;
		keys = new boolean[KeyEvent.KEY_LAST+1];
		buttonUp = new ImageIcon("buttons/button1_up.png").getImage();
		buttonDown = new ImageIcon("buttons/button1_down.png").getImage();
	 	
	    boxx = 170;
        boxy = 170;
		screen = MENU;
		
    	menuInstructions = new Rectangle(300,300,200,50);
    	menuPlay = new Rectangle(300,400,200,50);
    	
		setPreferredSize( new Dimension(800, 600));
        addKeyListener(this);
        addMouseListener(this);
	}
	
    public void addNotify() {
        super.addNotify();
        setFocusable(true);
        requestFocus();
        mainFrame.start();
    }
	
	// I might want to do something if it is menu too
	public void update(){
		if(screen == GAME){
			move();
		}
		mouse = MouseInfo.getPointerInfo().getLocation();
		Point offset = getLocationOnScreen();
		mouse.translate(-offset.x, -offset.y);
	}
	
	public void move(){
		if(keys[KeyEvent.VK_RIGHT] ){
			boxx += 5;
		}
		if(keys[KeyEvent.VK_LEFT] ){
			boxx -= 5;
		}
		if(keys[KeyEvent.VK_UP] ){
			boxy -= 5;
		}
		if(keys[KeyEvent.VK_DOWN] ){
			boxy += 5;
		}
	}
	
    public void keyTyped(KeyEvent e) {}

    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;
    }
    
    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;
    }
	public void	mouseClicked(MouseEvent e){}
	public void	mouseEntered(MouseEvent e){}
	public void	mouseExited(MouseEvent e){}
	
	public void	mousePressed(MouseEvent e){
		if(screen == MENU){
			if(menuInstructions.contains(mouse)){
				screen = INSTRUCTIONS;	
			}
			if(menuPlay.contains(mouse)){
				screen = GAME;
			}
		}		
	}
	
	public void	mouseReleased(MouseEvent e){}


    public void imageInRect(Graphics g, Image img, Rectangle area){
		g.drawImage(img, area.x, area.y, area.width, area.height, null);
    }
    
    public void drawMenu(Graphics g){
		g.setColor(new Color(0xB1C4DF));  
		g.fillRect(0,0,800,600);
		if(menuInstructions.contains(mouse)){
			imageInRect(g, buttonUp, menuInstructions);
		}
		else{
			imageInRect(g, buttonDown, menuInstructions);			
		}
		if(menuPlay.contains(mouse)){
			imageInRect(g, buttonUp, menuPlay);
		}
		else{
			imageInRect(g, buttonDown, menuPlay);
		}
    }

    public void drawGame(Graphics g){
		g.setColor(new Color(0xB1DFC4));
		g.fillRect(0,0,800,600);
		g.setColor(Color.blue);
		g.fillRect(boxx,boxy,40,40);
    }

    public void drawInstructions(Graphics g){
		g.setColor(new Color(0xDFB1C4));
		g.fillRect(0,0,800,600);
	}

    public void paint(Graphics g){
    	if(screen == MENU){
    		drawMenu(g);
    	}
    	else if(screen == GAME){
    		drawGame(g);
    	}
    	else if(screen == INSTRUCTIONS){
    		drawInstructions(g);
    	}
    }
}