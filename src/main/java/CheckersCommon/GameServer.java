package CheckersCommon;

import java.rmi.RemoteException;
import java.util.List;

/**
 * Created by Slid on 4/15/2016.
 */
public interface GameServer extends java.rmi.Remote {

    Menu login(String username, String password) throws RemoteException;

    PlayersGame createGame(UserData user, String gameName) throws RemoteException;

    List<String> listCurrentGames() throws RemoteException;

    Game retrieveGame(String gameName) throws RemoteException;

}
