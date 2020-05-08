import javax.swing.*;
import java.awt.*;

class PanelExtension {
    static ListCellRenderer<? super String> messageListRenderer(Font textFont) {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (c instanceof JLabel) {
                    JLabel label = (JLabel) c;
                    String message = (String) value;
                    label.setFont(textFont);
                    if (index % 2 != 0){
                        label.setBackground(new Color(235, 235, 235));
                    }
                    if (message.contains("~")) {
                        String[] messageParts = message.split("~");
                        label.setForeground(Color.decode(messageParts[0]));
                        label.setText(messageParts[1]);
                    } else {
                        label.setForeground(Color.black);
                        label.setText(message);
                    }
                }
                return c;
            }
        };
    }

    static ListCellRenderer<? super Player> playerListRenderer(Icon avatar, Font textFont, Player myPlayer){
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (c instanceof JLabel) {
                    JLabel label = (JLabel) c;
                    label.setFont(textFont);
                    label.setIcon(avatar);
                    if (index % 2 != 0){
                        label.setBackground(new Color(235, 235, 235));
                    }
                    Player p = (Player) value;
                    if (p == myPlayer){
                        label.setText("<html>"+p.getName()+" (You)"+"<br>"+"Points: "+p.getScore()+"</html>");
                    }else {
                        label.setText("<html>" + p.getName() + "<br>" +"Points: "+ p.getScore() + "</html>");
                    }
                    label.setFont(textFont);
                }
                return c;
            }
        };
    }

    static ListCellRenderer<? super Player> pointsGainedNameListRenderer(Font textFont){
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (c instanceof JLabel) {
                    JLabel label = (JLabel) c;
                    label.setBackground(new Color(235, 235, 235));
                    label.setFont(textFont.deriveFont(35f));
                    Player p = (Player)value;
                    label.setText(p.getName());
                }
                return c;
            }
        };
    }

    static ListCellRenderer<? super Player> pointsGainedPointListRenderer(Font textFont){
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (c instanceof JLabel) {
                    JLabel label = (JLabel) c;
                    label.setFont(textFont.deriveFont(35f));
                    label.setBackground(new Color(235, 235, 235));
                    Player p = (Player)value;
                    if (p.getPointsGainedLastRound() == 0){
                        label.setText(""+p.getPointsGainedLastRound());
                        label.setForeground(Color.red);
                    }else{
                        label.setText("+"+p.getPointsGainedLastRound());
                        label.setForeground(new Color(40, 200, 40));
                    }
                    label.setHorizontalAlignment(SwingConstants.CENTER);

                }
                return c;
            }
        };
    }
}