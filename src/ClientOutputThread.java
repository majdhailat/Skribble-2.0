import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

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
                //NON ARTIST MODE
                String usersTextMessage = client.getUsersTextMessage();
                if (!client.getDataPackage().getMyPlayer().isArtist()) {
                    if (usersTextMessage != null) {
                        try {
                            out.writeObject(usersTextMessage);
                            out.flush();
                        } catch (IOException e) {e.printStackTrace();}
                    }
                }
                //ARTIST MODE
                else{
                    while(client.getDataPackage().getMyPlayer().isArtist() && client.getDataPackage().getGameStatus().equals(DataPackage.ROUNDINPROGRESS)){//starting artist loop
                        try {
                            TimeUnit.MILLISECONDS.sleep(200);
                        } catch (InterruptedException e) {e.printStackTrace();}
                        try {
                            out.writeObject(client.getDrawingComponents());//sending drawing components array
                            out.flush();
                            out.reset();
                        } catch (IOException e) {e.printStackTrace();}
                    }
                    try {
                        out.writeObject(0);//"band aid" fix
                        client.updateUsersTextMessage(null);
                    } catch (IOException e) {e.printStackTrace();}
                }
            }
        }
    }
}
