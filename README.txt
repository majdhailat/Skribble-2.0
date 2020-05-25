To run our game you must first run the server then the clients.

You must run at least 2 clients to actually play the game.

On IntelliJ you can run multiple instances of the same code, so you 
don't have to create new client code fo every additional client. 
But I don't know if that works on other IDEs.

Once you have the server running and at least 2 clients, you must type start 
into the chat box of the host (the first client you ran), it will prompt you 
to do so anyways.


If you want to have the clients on separate machines than the server you must change the hostname inside the client from "localhost" to whatever the host name of the computer is followed by ".local". Our game only works if the server and clients are on the same network.