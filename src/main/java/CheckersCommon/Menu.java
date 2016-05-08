package CheckersCommon;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * Created by Slid on 4/15/2016.
 */
public class Menu implements java.io.Serializable {
    private final UserData user;
    private final GameServer gameServer;
    boolean activeMenu;

    public Menu(UserData user, GameServer server) throws RemoteException{
        this.user = user;
        this.gameServer = server;
        this.activeMenu = true;
    }

    public synchronized PlayersGame createGame(String gameName) throws RemoteException {
        if(!activeMenu)
            throw new IllegalStateException("User has logged out");
        if(user.isInGame())
            throw new IllegalStateException("User already in game");
        return gameServer.createGame(user, gameName);
    }

    public synchronized List<String> listCurrentGames() throws RemoteException {
        if(!activeMenu)
            throw new IllegalStateException("User has logged out");
        return gameServer.listCurrentGames();
    }

    public synchronized PlayersGame joinGame(String gameName) throws RemoteException {
        if(!activeMenu)
            throw new IllegalStateException("User has logged out");
        if(user.isInGame())
            throw new IllegalStateException("User already in game");
        Game game = gameServer.retrieveGame(gameName);
        if(game != null) {
            return game.joinGame(user);
        }
        else
            return null;
    }

    public synchronized Map<String, Integer> userStatistics() throws RemoteException {
        if(!activeMenu)
            throw new IllegalStateException("User has logged out");
        return user.userStatistics();
    }

    public synchronized boolean logout() throws RemoteException {
        if(!activeMenu)
            throw new IllegalStateException("User has logged out");
        boolean loggedOut = user.logout();
        if(loggedOut){
            activeMenu = false;
            return true;
        }
        return false;
    }
}
