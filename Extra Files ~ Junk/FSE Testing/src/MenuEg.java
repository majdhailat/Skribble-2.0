/* MenuEg
 *
 * This is the standard frame setup for the menuExample.
 *
 **/

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class MenuEg extends JFrame implements ActionListener{
	Timer myTimer;   
	GamePanel game;
		
    public MenuEg() {
		super("Move the Box");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		myTimer = new Timer(10, this);	
		
		game = new GamePanel(this);
		add(game);
		pack();
		setVisible(true);
    }
    
	public void start(){
		myTimer.start();
	}
	
	public void actionPerformed(ActionEvent evt){
		if(game != null){
			game.update();
			game.repaint();
		}
	}

    public static void main(String[] arguments) {
		MenuEg frame = new MenuEg();		
    }
}
