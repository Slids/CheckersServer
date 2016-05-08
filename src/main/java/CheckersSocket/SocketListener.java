package CheckersSocket;

import CheckersCommon.Game;
import CheckersCommon.GameServer;
import CheckersCommon.Menu;
import CheckersCommon.PlayersGame;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by Slid on 4/26/2016.
 */
public class SocketListener implements Runnable {

    private final ServerSocket serv;
    private final ExecutorService es;
    private final GameServer gs;
    private final ConcurrentHashMap<Long, Menu> userMenus;
    private final ConcurrentHashMap<Long, PlayersGame> userGames;

    public SocketListener(int port, String gsHost, int gsPort, String checkersgsName) throws IOException, java.rmi.NotBoundException {
        System.out.println("Listening on port: " + port);
        this.serv = new ServerSocket(port);
        this.userMenus = new ConcurrentHashMap<>();
        this.userGames = new ConcurrentHashMap<>();

        Registry registry = LocateRegistry.getRegistry(gsHost, gsPort);
        this.gs = (GameServer) registry.lookup(checkersgsName);
        es = Executors.newCachedThreadPool();
    }

    public void run() {
        boolean keepRunning = true;
        while (keepRunning) {
            try {
                System.out.println("Starting to Accept");
                Socket s = serv.accept();
                System.out.println("Accepted: " + s.toString());
                es.execute(new ConnectionHandler(s, gs, userMenus, userGames, serv));
            } catch (IOException ex) {
                System.out.println(ex.toString());
                es.shutdown();
                try {
                    es.awaitTermination(1000, TimeUnit.MINUTES);
                }catch(InterruptedException ex2){
                    System.err.println(ex2.toString());
                }
                for(PlayersGame game : userGames.values()){
                    try {
                        game.leaveGame();
                    }catch(RemoteException ex2){System.err.println(ex.toString());}
                }
                for(Menu menu : userMenus.values()){
                    try {
                        menu.logout();
                    }catch(RemoteException ex2){System.err.println(ex.toString());}
                }
                //Users have been removed, games have been closed, goodbye
                System.out.println("Good bye");
                keepRunning = false;
            }
        }
    }
}
