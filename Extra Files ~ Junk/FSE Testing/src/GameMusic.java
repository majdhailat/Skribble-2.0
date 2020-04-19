import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.io.*;
import javax.sound.midi.*;

public class GameMusic extends JFrame{
	Timer myTimer;   
	GamePanel game;

   private static Sequencer midiPlayer;
   
   public static void startMidi(String midFilename) {
      try {
         File midiFile = new File(midFilename);
         Sequence song = MidiSystem.getSequence(midiFile);
         midiPlayer = MidiSystem.getSequencer();
         midiPlayer.open();
         midiPlayer.setSequence(song);
         midiPlayer.setLoopCount(0); // repeat 0 times (play once)
         midiPlayer.start();
      } catch (MidiUnavailableException e) {
         e.printStackTrace();
      } catch (InvalidMidiDataException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }	
	
    public GameMusic() {
		super("Music to my ears");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(800,650);
		
		startMidi("trippygaia1.mid");     // start the midi player
		
		setResizable(false);
		setVisible(true);
    }
    
	
    public static void main(String[] arguments) {
		GameMusic frame = new GameMusic();		
    }
}
