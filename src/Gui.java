//imports
import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

/*
Creates the frame of the window
Prepares and starts the panel
Handles window closing (pressed X button)
Updates panel with new data from client and repaints client every 100ms
 */
public class Gui extends JFrame {
    private Panel panel;
    public Gui(Client client) throws IOException{
        super("~~~ Scribble ~~~");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(1280, 720));//setting frame size
        Timer myTimer = new Timer(100, new TickListener());// trigger every 100 ms. used to refresh graphics
        myTimer.start();//starting timer
        try {
            panel = new Panel(client);//starting panel
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        add(panel);
        setResizable(false);
        setVisible(true);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {//executed when window is closed -> used to terminate client threads
                client.end();
            }
        });
    }

    class TickListener implements ActionListener {
        public void actionPerformed(ActionEvent evt) {//triggered every 100 ms by myTimer
            if (panel != null && panel.ready) {
                panel.updateFromClient();
                panel.repaint();
            }
        }
    }
}