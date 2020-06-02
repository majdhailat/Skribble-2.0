//imports
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/*
Stores the cell renderers - message cells and the player cells on the left - used by the client
Cell renderers handle the appearance of each cell in the table
 */
class PanelExtension {
    /*
    cell renderer for the messages cells on the right
    Sets the actual message of the cell
    Sets the color of the message by decoding the color hidden within the message itself
    Sets the cell backgrounds to alternate in colour (white and grey)
    Sets text font
     */
    static ListCellRenderer<? super String> messageListRenderer(Font textFont) {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (c instanceof JLabel) {
                    JLabel label = (JLabel) c;//message label
                    String message = (String) value;
                    label.setFont(textFont);//setting font
                    if (index % 2 != 0){//alternating cells
                        label.setBackground(new Color(235, 235, 235));//changing every other cell color for contrast
                    }
                    if (message.contains("~")) {//looking to see if the message has a specified color
                        String[] messageParts = message.split("~");
                        label.setForeground(Color.decode(messageParts[0]));//getting and setting color for message
                        label.setText(messageParts[1]);//setting message text
                    } else {//no specified color
                        label.setForeground(Color.black);//setting message color to black
                        label.setText(message);//setting message text
                    }
                }
                return c;//returning cell
            }
        };
    }

    /*
    Cell renderer for the player cells in the left
     */
    static ListCellRenderer<? super Player> playerListRenderer(Font textFont, Player myPlayer){
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (c instanceof JLabel) {
                    JLabel label = (JLabel) c;//player name label
                    Player p = (Player) value;
                    label.setFont(textFont);


                    if (index % 2 != 0){//alternating cells
                        label.setBackground(new Color(235, 235, 235));//changing every other cell color for contrast
                    }
                    label.setIcon(PanelExtension.getIcon(p.getIconImageNumber()));//setting player icon

                    if (p.getUniqueID() == myPlayer.getUniqueID()){//checking for my player
                        label.setText("<html>"+p.getName()+" (You)"+"<br>"+"Points: "+p.getScore()+"</html>");//setting name and (you)
                    }else {
                        if (p.isArtist()){
                            label.setText("<html>" + p.getName() + " (Artist)"+"<br>" +"Points: "+ p.getScore() + "</html>");//setting name and artist indicator
                        }else{
                            label.setText("<html>" + p.getName() +"<br>" +"Points: "+ p.getScore() + "</html>");//setting name
                        }

                    }
                }
                return c;
            }
        };
    }
    private static ImageIcon[] icons = new ImageIcon[]{null, null, null, null, null, null};
    private static ImageIcon getIcon(int iconNumber){
        if(icons[iconNumber - 1] == null){
            icons[iconNumber] = new ImageIcon("image assets/icons/icon"+iconNumber+".jpg");
        }
        return icons[iconNumber];
    }

}