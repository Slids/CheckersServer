package CheckersServer;

import CheckersCommon.GameServer;
import CheckersCommon.GameServerImp;
import CheckersSocket.SocketListener;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Created by Slid on 4/19/2016.
 */
public class GameServerRun {

    public static final String SERVER_HOST = "localhost";
    public static final String SERVER_NAME = "ServerOfCheckers";

    private final GameServerImp checkerServer;
    private Registry registry;

    /**
     * Creates a server for the given bank.
     */
    public GameServerRun(GameServerImp check) {
        this.checkerServer = check;
    }

    /**
     * Starts the server by binding it to a registry.
     *
     * <ul>
     *
     * <li>If {@code port} is positive, the server attempts to locate a registry at this port.</li>
     *
     * <li>If {@code port} is negative, the server attempts to start a new registry at this
     * port.</li>
     *
     * <li>If {@code port} is 0, the server attempts to start a new registry at a randomly chosen
     * port.</li>
     *
     * </ul>
     *
     * @return the registry port
     */
    public synchronized int start(int port) throws RemoteException {
        if (registry != null)
            throw new IllegalStateException("server already running");
        Registry reg;
        if (port > 0) { // registry already exists
            reg = LocateRegistry.getRegistry(port);
        } else if (port < 0) { // create on given port
            port = -port;
            reg = LocateRegistry.createRegistry(port);
        } else { // create registry on random port
            Random rand = new Random();
            int tries = 0;
            while (true) {
                port = 50000 + rand.nextInt(10000);
                try {
                    reg = LocateRegistry.createRegistry(port);
                    break;
                } catch (RemoteException e) {
                    if (++tries < 10 && e.getCause() instanceof java.net.BindException)
                        continue;
                    throw e;
                }
            }
        }
        reg.rebind(checkerServer.getName(), checkerServer);
        registry = reg;
        return port;
    }

    /**
     * Stops the server by removing the bank form the registry.  The bank is left exported.
     */
    public synchronized void stop() {
        if (registry != null) {
            try {
                registry.unbind(checkerServer.getName());
            } catch (Exception e) {
                System.err.printf("unable to stop: %s%n", e.getMessage());
            } finally {
                registry = null;
            }
        }
    }

    /**
     * Prints a bank status (all accounts with their balances).
     */
    public synchronized void printStatus() {
        System.out.printf("%nGame status:");
        List<String> games = checkerServer.listCurrentGames();
        if (games.isEmpty()) {
            return;
        }
        System.out.println();
        for (String e : games)
            System.out.println(e);
    }

    /**
     * Command-line program.  Single (optional) argument is a port number (see {@link #start(int)}).
     */
    public static void main(String[] args) throws Exception {
        int port = 0;
        if (args.length > 0)
            port = Integer.parseInt(args[0]);
        GameServerImp gameServer = new GameServerImp(SERVER_NAME);
        GameServerRun server = new GameServerRun(gameServer);


        try {
            //port = server.start(port);
            port = server.start(-5387);

            //Start socket guy
            ExecutorService ex =  Executors.newCachedThreadPool();
            ex.submit(new SocketListener(port-1,SERVER_HOST,port,SERVER_NAME));


            System.out.printf("server running on port %d%n", port);
            ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
            exec.scheduleAtFixedRate(server::printStatus, 1, 1, MINUTES);
        } catch (RemoteException e) {
            Throwable t = e.getCause();
            if (t instanceof java.net.ConnectException)
                System.err.println("unable to connect to registry: " + t.getMessage());
            else if (t instanceof java.net.BindException)
                System.err.println("cannot start registry: " + t.getMessage());
            else
                System.err.println("cannot start server: " + e.getMessage());
            UnicastRemoteObject.unexportObject(gameServer, false);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
}