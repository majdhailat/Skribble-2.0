import javax.xml.crypto.Data;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class ClientOutputThread extends Thread {
    //The stream is used to send both the drawing components and the users string message
    private ObjectOutputStream out;

    private Client client;


    public ClientOutputThread(Socket socket, Client client) throws IOException {
        out = new ObjectOutputStream(socket.getOutputStream());
        this.client = client;
    }

    //if the message that tells the user to type start has been prompted

    public void run() {
        while (client.isRunning()){
            DataPackage dataPackage = client.getDataPackage();
            if (dataPackage != null) {


                //NON ARTIST MODE
                String usersTextMessage = client.getUsersTextMessage();
                System.out.println("client output thread asking for msg");
                if (!dataPackage.getMyPlayer().isArtist()) {


                    if (usersTextMessage != null) {
                        try {
                            out.writeObject(usersTextMessage);
                            out.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                //ARTIST MODE
                else if (dataPackage.getMyPlayer().isArtist()){//checking if the user is an artist
                    while(dataPackage.getMyPlayer().isArtist() && dataPackage.getGameStatus().equals(DataPackage.ROUNDINPROGRESS)){//starting artist loop
                        try {
                            TimeUnit.MILLISECONDS.sleep(100);
                        } catch (InterruptedException e) {e.printStackTrace();}
                        try {
                            out.writeObject(client.getDrawingComponents());//sending drawing components array
                            out.flush();
                            out.reset();//may be unnecessary
                        } catch (IOException e) {e.printStackTrace();}
                    }
                    try {
                        out.writeObject(0);//this is a "band aid" fix
                        client.updateUsersTextMessage(null);

                    } catch (IOException e) {e.printStackTrace();}
                }
            }
        }
    }
}
