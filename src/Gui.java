import javax.sound.midi.MidiUnavailableException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class Gui extends JFrame {
    private Panel panel;
    public Gui(Client client) throws IOException{
        super("Skribble");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(1280, 720));
        Timer myTimer = new Timer(100, new TickListener());// trigger every 100 ms. used to refresh graphics
        myTimer.start();
        panel = new Panel(client);

        add(panel);
        setResizable(false);
        setVisible(true);
        //this line of code executes anything inside the curly brackets when the user closes the window
        //we are setting running to false to close all threads properly
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                //running = false;
            }
        });

    }

    //triggered every 100 ms by myTimer
    private boolean didRun = false;
    class TickListener implements ActionListener {
        public void actionPerformed(ActionEvent evt) {
            if (panel != null && panel.ready) {
                panel.updateFromClient();
                panel.repaint();
            }
        }
    }
}