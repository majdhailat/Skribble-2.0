import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/*
If the client is not an artist:
    - Gets any new user text message from client, encodes the message and send to server
If the client is an artist:
    - Gets drawing components from client, encodes the array and sends to server
 */
class ClientOutputThread extends Thread {
    private ObjectOutputStream out;//stream used to send drawing components and the users message
    private Client client;

    public ClientOutputThread(Socket socket, Client client) throws IOException {
        out = new ObjectOutputStream(socket.getOutputStream());
        this.client = client;
    }

    public void run() {
        while (client.isRunning()){
            if (client.getDataPackage() != null) {
                try {
                    out.writeObject(0);//triggers exception when the client switches from artist to non artist
                } catch (IOException e) {e.printStackTrace();}
                client.updateUsersTextMessage(null);

                //NON ARTIST MODE
                String usersTextMessage = client.getUserTextMessage();//getting message from client
                if (!client.getDataPackage().getMyPlayer().isArtist()) {//checking if not artist
                    if (usersTextMessage != null) {
                        try {
                            out.writeObject(usersTextMessage);//sending message
                            out.flush();
                        } catch (IOException e) {e.printStackTrace();}
                    }
                }
                //ARTIST MODE
                else{
                    while(client.getDataPackage().getMyPlayer().isArtist() && client.getDataPackage().getGameStatus().equals(DataPackage.ROUNDINPROGRESS)) {//starting artist loop
                        try {
                            TimeUnit.MILLISECONDS.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        try {
                            out.writeObject(client.getDrawingComponents());//sending drawing components array
                            out.flush();
                            out.reset();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
